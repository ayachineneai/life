package life.app.ai.conversation

import life.util.Times
import life.util.Uuids
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

class ConversationService(
    private val conversationDao: ConversationDao,
) {
    fun createConversation(
        openaiConversationId: String,
        title: String? = null,
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
            check(conversationDao.updateLastActiveTime(conversationId, now)) {
                "Conversation not found: $conversationId"
            }
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
