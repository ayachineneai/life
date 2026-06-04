package life.app.ai.demo

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.cdimascio.dotenv.Dotenv
import life.app.ai.tool.ToolRegistry
import life.app.ai.tool.handler.ToolCallHandler
import life.app.ai.tool.protocol.ToolOutput
import life.app.ai.tool.scan.ToolScanner
import life.app.ai.utils.AiRequests
import life.app.ai.utils.AiRuns
import life.app.ai.utils.AiStreamEventHandlers
import life.app.ai.utils.outputText
import life.app.di.appModule
import life.app.modules.meal.MealTable
import life.infra.db.DbConfig
import life.infra.proxy.ProxyConfig
import life.util.Times
import life.util.optional
import org.koin.core.context.startKoin
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

private val mapper = JsonMapper.builder().findAndAddModules().build()
private val dotenv = Dotenv.configure().ignoreIfMissing().load()

fun main() {
    val port = env("AI_DEMO_PORT")?.toIntOrNull() ?: 7070
    connectDemoDatabase()
    val koin = startKoin { modules(appModule) }.koin
    val registry = ToolScanner.scan("life")
    val toolHandler = ToolCallHandler(registry, koin)
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.executor = Executors.newCachedThreadPool()
    server.createContext("/") { exchange ->
        when {
            exchange.requestMethod == "GET" && exchange.requestURI.path == "/" -> exchange.sendUi()
            exchange.requestMethod == "GET" && exchange.requestURI.path == "/api/tools" -> exchange.sendTools(registry)
            exchange.requestMethod == "POST" && exchange.requestURI.path == "/api/chat" -> {
                exchange.streamAi(registry, toolHandler)
            }
            else -> exchange.sendText(404, "Not found")
        }
    }
    server.start()
    println("AI streaming demo: http://localhost:$port")
}

private fun connectDemoDatabase() {
    val config = dbConfig()
    Database.connect(
        url = config.url,
        driver = config.driver ?: dbDriver(config.url),
        user = config.username,
        password = config.password,
    )
    transaction {
        SchemaUtils.createMissingTablesAndColumns(MealTable)
    }
    println("AI streaming demo database: ${redactDbUrl(config.url)}")
}

private fun HttpExchange.streamAi(registry: ToolRegistry, toolHandler: ToolCallHandler) {
    val request = readChatRequest()
    if (request.input.isBlank()) return sendText(400, "Input is required")

    responseHeaders.add("Content-Type", "text/event-stream; charset=utf-8")
    responseHeaders.add("Cache-Control", "no-cache")
    responseHeaders.add("Connection", "keep-alive")
    sendResponseHeaders(200, 0)

    responseBody.use { out ->
        try {
            out.writeSse("tools", tools(registry))
            runToolLoop(request, registry, toolHandler, out)
        } catch (e: Exception) {
            out.writeSse("error", mapOf("message" to (e.message ?: e::class.simpleName)))
        }
    }
}

private fun HttpExchange.readChatRequest(): ChatRequest {
    val body = requestBody.use { String(it.readAllBytes(), UTF_8) }.trim()
    if (!body.startsWith("{")) return ChatRequest(input = body)
    return runCatching { mapper.readValue(body, ChatRequest::class.java) }
        .getOrElse { ChatRequest(input = body) }
}

private fun runToolLoop(
    request: ChatRequest,
    registry: ToolRegistry,
    toolHandler: ToolCallHandler,
    out: OutputStream,
) {
    val client = openAiClient()
    val requestedConversationId = request.conversationId?.takeIf { it.isNotBlank() }
    val conversationId = requestedConversationId ?: client.conversations().create().id()
    out.writeSse("conversation", mapOf(
        "id" to conversationId,
        "reused" to (requestedConversationId != null),
    ))
    var params = AiRequests.conversationText(
        conversationId = conversationId,
        text = request.input,
        systemPrompt = toolDemoPrompt(),
        tools = registry.openAiTools(),
    )
    repeat(6) {
        val streamedText = StringBuilder()
        val textHandler = AiStreamEventHandlers.text(
            onText = { text ->
                streamedText.append(text)
                out.writeSse("text", mapOf("delta" to text))
            },
            onError = { message -> out.writeSse("error", mapOf("message" to message)) },
        )
        val response = AiRuns.runStream(
            client = client,
            params = params,
            handler = { event ->
                textHandler(event)
                event.functionCallArgumentsDelta().ifPresent { delta ->
                    out.writeSse("tool_call_delta", mapOf(
                        "itemId" to delta.itemId(),
                        "outputIndex" to delta.outputIndex(),
                        "delta" to delta.delta(),
                    ))
                }
                event.functionCallArgumentsDone().ifPresent { done ->
                    out.writeSse("tool_call_ready", mapOf(
                        "itemId" to done.itemId(),
                        "outputIndex" to done.outputIndex(),
                        "name" to runCatching { done.name() }.getOrNull(),
                        "arguments" to done.arguments(),
                    ))
                }
            },
        )
        if (streamedText.isEmpty()) {
            val fallbackText = response.outputText()
            if (fallbackText.isNotBlank()) {
                out.writeSse("text", mapOf("delta" to fallbackText))
            }
        }
        val outputs = response.output().mapNotNull { call ->
            call.functionCall().ifPresent { functionCall ->
                out.writeSse("tool_call", mapOf(
                    "callId" to functionCall.callId(),
                    "name" to functionCall.name(),
                    "arguments" to functionCall.arguments(),
                ))
            }
            toolHandler.handle(call)?.also { output -> out.writeSse("tool_result", toolResult(output)) }
        }
        if (outputs.isEmpty()) {
            out.writeSse("done", mapOf("responseId" to response.id()))
            return
        }
        params = AiRequests.toolOutputs(
            outputs = outputs,
            conversationId = conversationId,
        )
    }
    out.writeSse("error", mapOf("message" to "Tool loop limit reached"))
}

private fun openAiClient(): OpenAIClient {
    val apiKey = env("OPENAI_API_KEY")
    return OpenAIOkHttpClient.builder()
        .apiKey(apiKey ?: error("Missing OPENAI_API_KEY"))
        .proxy(proxyConfig().toProxy())
        .build()
}

private fun proxyConfig(): ProxyConfig {
    return ProxyConfig(
        host = env("PROXY_HOST") ?: "127.0.0.1",
        port = env("PROXY_PORT")?.toIntOrNull() ?: 7890,
    )
}

private fun dbConfig(): DbConfig {
    val url = env("DB_URL") ?: env("DATABASE_URL") ?: defaultPostgresUrl()
    return DbConfig(
        url = url,
        username = env("DB_USERNAME") ?: env("DB_USER") ?: defaultDbUsername(url),
        password = env("DB_PASSWORD") ?: defaultDbPassword(url),
        driver = env("DB_DRIVER"),
    )
}

private fun defaultPostgresUrl(): String {
    return "jdbc:postgresql://localhost:5432/life"
}

private fun defaultDbUsername(url: String): String {
    return when {
        url.startsWith("jdbc:postgresql:") -> "postgres"
        url.startsWith("jdbc:h2:") -> "sa"
        else -> ""
    }
}

private fun defaultDbPassword(url: String): String {
    return if (url.startsWith("jdbc:postgresql:")) "123456" else ""
}

private fun dbDriver(url: String): String {
    return when {
        url.startsWith("jdbc:h2:") -> "org.h2.Driver"
        url.startsWith("jdbc:postgresql:") -> "org.postgresql.Driver"
        else -> error("Missing DB_DRIVER for database URL: ${redactDbUrl(url)}")
    }
}

private fun redactDbUrl(url: String): String {
    return url
        .replace(Regex("(?i)(password=)[^&;]+"), "$1***")
        .replace(Regex("(?i)(user=)[^&;]+"), "$1***")
}

private fun env(name: String): String? {
    return dotenv.optional(name) ?: System.getenv(name)?.takeIf { it.isNotBlank() }
}

private fun toolDemoPrompt(): String {
    val now = Times.now()
    return """
        You are running a tool streaming demo. If the user asks to verify tools, call demo_echo.
        Meal tool timestamps must use Asia/Shanghai local time in yyyy-MM-dd'T'HH:mm:ss format, with no timezone offset.
        Do not send Z or +08:00 for meal timestamps. Current Asia/Shanghai time is $now.
        After a tool result is returned, briefly explain what happened.
    """.trimIndent()
}

private fun tools(registry: ToolRegistry): List<ToolInfo> {
    return registry.tools.map { tool ->
        ToolInfo(
            name = tool.name,
            description = tool.description,
            parameters = tool.paramsSchema,
        )
    }
}

private fun toolResult(output: ToolOutput): Map<String, String> {
    return mapOf(
        "callId" to output.callId,
        "output" to mapper.writeValueAsString(output.result),
    )
}

private fun HttpExchange.sendUi() {
    val html = Files.readString(Paths.get("ui", "ai-stream-demo.html"))
    responseHeaders.add("Content-Type", "text/html; charset=utf-8")
    sendResponseHeaders(200, html.toByteArray(UTF_8).size.toLong())
    responseBody.use { it.writeText(html) }
}

private fun HttpExchange.sendTools(registry: ToolRegistry) {
    val json = mapper.writeValueAsString(tools(registry))
    responseHeaders.add("Content-Type", "application/json; charset=utf-8")
    sendResponseHeaders(200, json.toByteArray(UTF_8).size.toLong())
    responseBody.use { it.writeText(json) }
}

private fun HttpExchange.sendText(status: Int, text: String) {
    responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
    sendResponseHeaders(status, text.toByteArray(UTF_8).size.toLong())
    responseBody.use { it.writeText(text) }
}

private fun OutputStream.writeSse(event: String, data: Any?) {
    writeText("event: $event\n")
    mapper.writeValueAsString(data).lineSequence().forEach { line ->
        writeText("data: $line\n")
    }
    writeText("\n")
}

private fun OutputStream.writeText(text: String) {
    write(text.toByteArray(UTF_8))
    flush()
}

private data class ToolInfo(
    val name: String,
    val description: String,
    val parameters: ObjectNode,
)

private data class ChatRequest(
    val input: String = "",
    val conversationId: String? = null,
)
