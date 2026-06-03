package life.app.ai

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime
import kotlin.uuid.Uuid

data class ConversationPo(
    val id: Uuid,
    val openaiConversationId: String,
    val title: String? = null,
    val createTime: LocalDateTime,
    val lastActiveTime: LocalDateTime,
)

data class ConversationTurnPo(
    val id: Uuid,
    val conversationId: Uuid? = null,
    val userInput: String? = null,
    val modelOutput: String? = null,
    val openaiResponseId: String? = null,
    val createTime: LocalDateTime,
)

object ConversationTable : Table("conversation") {
    val id = uuid("id")
    val openaiConversationId = varchar("openai_conversation_id", 128)
    val title = varchar("title", 128).nullable()
    val createTime = datetime("create_time")
    val lastActiveTime = datetime("last_active_time")

    override val primaryKey = PrimaryKey(id)
}

object ConversationTurnTable : Table("conversation_turn") {
    val id = uuid("id")
    val conversationId = uuid("conversation_id").nullable()
    val userInput = text("user_input").nullable()
    val modelOutput = text("model_output").nullable()
    val openaiResponseId = varchar("openai_response_id", 128).nullable()
    val createTime = datetime("create_time")

    override val primaryKey = PrimaryKey(id)
}
