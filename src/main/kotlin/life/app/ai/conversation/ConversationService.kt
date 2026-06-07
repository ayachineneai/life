package life.app.ai.conversation

import com.openai.client.OpenAIClient
import life.util.Times
import life.util.Uuids
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

class ConversationService(
    private val conversationDao: ConversationDao,
    private val client: OpenAIClient,
) {
    fun createConversation(title: String): ConversationPo {
        return createConversation(
            openaiConversationId = client.conversations().create().id(),
            title = title,
        )
    }

    fun createConversation(
        openaiConversationId: String,
        title: String,
    ): ConversationPo {
        val now = Times.now()
        val conversation = ConversationPo(
            id = Uuids.uuid7(),
            openaiConversationId = openaiConversationId,
            title = title,
            createTime = now,
            lastActiveTime = now,
        )

        transaction {
            conversationDao.insertConversation(conversation)
        }
        return conversation
    }

    fun findConversationById(id: String): ConversationPo? {
        return findConversationById(Uuids.parse(id))
    }

    fun findConversationById(id: Uuid): ConversationPo? {
        return transaction {
            conversationDao.findConversationById(id)
        }
    }

    fun listConversations(limit: Int = 10): List<ConversationPo> {
        return transaction {
            conversationDao.listConversations(limit)
        }
    }

    fun updateTitle(id: Uuid, title: String?): Boolean {
        return transaction {
            conversationDao.updateTitle(id, title)
        }
    }

    fun appendTurn(
        conversationId: Uuid,
        userInput: String,
        modelOutput: String,
        openaiResponseId: String? = null,
        model: String? = null,
    ): ConversationTurnPo {
        val now = Times.now()
        val turn = ConversationTurnPo(
            id = Uuids.uuid7(),
            conversationId = conversationId,
            userInput = userInput,
            modelOutput = modelOutput,
            openaiResponseId = openaiResponseId,
            model = model,
            createTime = now,
        )

        transaction {
            checkNotNull(conversationDao.findConversationById(conversationId)) {
                "Conversation not found: $conversationId"
            }
            conversationDao.insertTurn(turn)
            conversationDao.updateLastActiveTime(conversationId, now)
        }

        return turn
    }

    fun listTurns(
        conversationId: Uuid,
        beforeId: Uuid? = null,
        limit: Int = 50,
    ): List<ConversationTurnPo> {
        return transaction {
            conversationDao.listTurnsByConversationId(conversationId, beforeId, limit)
        }
    }
}
