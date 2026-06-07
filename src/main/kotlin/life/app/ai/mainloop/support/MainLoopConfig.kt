package life.app.ai.mainloop.support

import com.openai.models.ChatModel
import life.app.ai.utils.AiRequests

data class MainLoopConfig(
    val plannerModel: ChatModel = AiRequests.chatModel,
    val executorModel: ChatModel = AiRequests.chatModel,
    val maxExecutorSteps: Int = 8,
    val titleMaxLength: Int = 40,
)
