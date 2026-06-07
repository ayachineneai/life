package life.app.ai.mainloop.support

import com.openai.client.OpenAIClient
import life.app.ai.conversation.ConversationService
import life.app.ai.mainloop.MainLoop
import life.infra.sse.SseWriter

class MainLoopFactory(
    private val client: OpenAIClient,
    private val conversationService: ConversationService,
    private val tools: MainLoopTools,
    private val config: MainLoopConfig,
) {
    fun create(sse: SseWriter): MainLoop {
        return MainLoop(
            client = client,
            conversationService = conversationService,
            tools = tools,
            config = config,
            sse = sse,
        )
    }
}
