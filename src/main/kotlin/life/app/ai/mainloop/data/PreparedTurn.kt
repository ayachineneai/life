package life.app.ai.mainloop.data

import kotlin.uuid.Uuid

data class PreparedTurn(
    val message: String,
    val conversationId: Uuid,
    val openAiConversationId: String,
)
