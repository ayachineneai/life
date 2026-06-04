package life.app.ai.demo

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.cdimascio.dotenv.Dotenv
import life.app.ai.utils.AiRequests
import life.app.ai.utils.AiRuns
import life.app.ai.utils.AiStreamEventHandlers
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

fun main() {
    val port = env("AI_DEMO_PORT")?.toIntOrNull() ?: 7070
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.executor = Executors.newCachedThreadPool()
    server.createContext("/") { exchange ->
        when {
            exchange.requestMethod == "GET" && exchange.requestURI.path == "/" -> exchange.sendUi()
            exchange.requestMethod == "POST" && exchange.requestURI.path == "/api/chat" -> exchange.streamAi()
            else -> exchange.sendText(404, "Not found")
        }
    }
    server.start()
    println("AI streaming demo: http://localhost:$port")
}

private fun HttpExchange.streamAi() {
    val input = requestBody.use { String(it.readAllBytes(), UTF_8) }.trim()
    if (input.isBlank()) return sendText(400, "Input is required")

    responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
    responseHeaders.add("Cache-Control", "no-cache")
    sendResponseHeaders(200, 0)

    responseBody.use { out ->
        try {
            AiRuns.runStream(
                client = openAiClient(),
                params = AiRequests.text(input),
                handler = AiStreamEventHandlers.text(
                    onText = { text -> out.writeText(text) },
                    onError = { message -> out.writeText("\n[error] $message") },
                ),
            )
        } catch (e: Exception) {
            out.writeText("\n[error] ${e.message ?: e::class.simpleName}")
        }
    }
}

private fun openAiClient(): OpenAIClient {
    val apiKey = env("OPENAI_API_KEY")
    return OpenAIOkHttpClient.builder()
        .apiKey(apiKey ?: error("Missing OPENAI_API_KEY"))
        .build()
}

private fun env(name: String): String? {
    val dotenv = Dotenv.configure().ignoreIfMissing().load()
    return (dotenv[name] ?: System.getenv(name))?.takeIf { it.isNotBlank() }
}

private fun HttpExchange.sendUi() {
    val html = Files.readString(Paths.get("ui", "ai-stream-demo.html"))
    responseHeaders.add("Content-Type", "text/html; charset=utf-8")
    sendResponseHeaders(200, html.toByteArray(UTF_8).size.toLong())
    responseBody.use { it.writeText(html) }
}

private fun HttpExchange.sendText(status: Int, text: String) {
    responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
    sendResponseHeaders(status, text.toByteArray(UTF_8).size.toLong())
    responseBody.use { it.writeText(text) }
}

private fun OutputStream.writeText(text: String) {
    write(text.toByteArray(UTF_8))
    flush()
}
