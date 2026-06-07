package life.main.startup

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import io.github.cdimascio.dotenv.Dotenv
import life.app.ai.conversation.ConversationService
import life.app.ai.tool.handler.ToolCallHandler
import life.app.ai.tool.scan.ToolScanner
import life.app.ai.mainloop.support.MainLoopConfig
import life.app.ai.mainloop.support.MainLoopFactory
import life.app.ai.mainloop.support.MainLoopTools
import life.app.di.appModule
import life.infra.proxy.ProxyConfig
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

data class AgentDependencies(
    val mainLoopFactory: MainLoopFactory,
    val conversationService: ConversationService,
    val mapper: ObjectMapper,
)

object Agent {
    fun start(dotenv: Dotenv): AgentDependencies {
        val mapper = JsonMapper.builder().findAndAddModules().build()
        val client = openAiClient(dotenv)
        val koin = startAppKoin(client)
        val registry = ToolScanner.scan("life")
        val tools = MainLoopTools(
            registry = registry,
            handler = ToolCallHandler(registry, koin),
        )
        val conversationService = koin.get<ConversationService>()

        return AgentDependencies(
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
        val builder = OpenAIOkHttpClient.builder()
            .apiKey(dotenv["OPENAI_API_KEY"])
            .proxy(proxyConfig(dotenv).toProxy())
        return builder.build()
    }

    private fun proxyConfig(dotenv: Dotenv): ProxyConfig {
        return ProxyConfig(
            host = dotenv["PROXY_HOST"],
            port = dotenv["PROXY_PORT"].toInt(),
        )
    }
}
