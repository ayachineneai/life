package life.app.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import io.github.cdimascio.dotenv.Dotenv
import life.app.ai.conversation.ConversationService
import life.app.ai.tool.handler.ToolCallHandler
import life.app.ai.tool.scan.ToolScanner
import life.app.ai.mainloop.support.AgentTools
import life.app.ai.mainloop.support.MainLoopConfig
import life.app.ai.mainloop.support.MainLoopFactory
import life.app.di.appModule
import life.infra.proxy.ProxyConfig
import life.util.optional
import life.util.required
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(AgentRuntime::class.java)

data class AgentRuntime(
    val mainLoopFactory: MainLoopFactory,
    val conversationService: ConversationService,
    val mapper: ObjectMapper,
)

object AgentRuntimes {
    fun create(dotenv: Dotenv = Dotenv.configure().ignoreIfMissing().load()): AgentRuntime {
        logger.info("Creating agent runtime")
        val mapper = JsonMapper.builder().findAndAddModules().build()
        val client = openAiClient(dotenv)
        val koin = startAppKoin(client)
        val registry = ToolScanner.scan(env(dotenv, "AGENT_TOOL_PACKAGE") ?: "life")
        logger.info(
            "Registered {} AI tools: {}",
            registry.tools.size,
            registry.tools.joinToString { tool -> tool.name },
        )
        val tools = AgentTools(
            registry = registry,
            handler = ToolCallHandler(registry, koin),
        )
        val conversationService = koin.get<ConversationService>()

        return AgentRuntime(
            mainLoopFactory = MainLoopFactory(
                client = client,
                conversationService = conversationService,
                tools = tools,
                config = MainLoopConfig(),
            ),
            conversationService = conversationService,
            mapper = mapper,
        )
    }

    fun port(dotenv: Dotenv): Int {
        return env(dotenv, "PORT")?.toIntOrNull()
            ?: env(dotenv, "AGENT_PORT")?.toIntOrNull()
            ?: 7070
    }

    private fun startAppKoin(client: OpenAIClient): Koin {
        return GlobalContext.getOrNull()
            ?: startKoin {
                modules(
                    module { single<OpenAIClient> { client } },
                    appModule,
                )
            }.koin
    }

    private fun openAiClient(dotenv: Dotenv): OpenAIClient {
        logger.info("Creating OpenAI client")
        val builder = OpenAIOkHttpClient.builder()
            .apiKey(dotenv.required("OPENAI_API_KEY"))
        proxyConfig(dotenv)?.let { proxy -> builder.proxy(proxy.toProxy()) }
        return builder.build()
    }

    private fun proxyConfig(dotenv: Dotenv): ProxyConfig? {
        val host = env(dotenv, "PROXY_HOST")
        if (host == null) {
            logger.info("No HTTP proxy configured for OpenAI client")
            return null
        }
        val port = env(dotenv, "PROXY_PORT")?.toIntOrNull() ?: 7890
        logger.info("Using HTTP proxy {}:{}", host, port)
        return ProxyConfig(host = host, port = port)
    }

    private fun env(dotenv: Dotenv, name: String): String? {
        return dotenv.optional(name) ?: System.getenv(name)?.takeIf { it.isNotBlank() }
    }
}
