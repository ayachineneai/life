package life.app.ai.conversation

import life.util.Times
import life.util.Uuids
import kotlin.uuid.Uuid

class ConversationService(
    private val conversationDao: ConversationDao = ConversationDao(),
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

        conversationDao.insertConversation(conversation)
        return conversation
    }

    fun findConversationById(id: Uuid): ConversationPo? {
        return conversationDao.findConversationById(id)
    }

    fun listConversations(): List<ConversationPo> {
        return conversationDao.listConversations()
    }

    fun updateTitle(id: Uuid, title: String?): Boolean {
        return conversationDao.updateTitle(id, title)
    }

    fun appendTurn(
        conversationId: Uuid,
        userInput: String,
        modelOutput: String,
        openaiResponseId: String? = null,
        model: String? = null,
    ): ConversationTurnPo {
        checkNotNull(conversationDao.findConversationById(conversationId)) {
            "Conversation not found: $conversationId"
        }
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

        conversationDao.insertTurn(turn)
        check(conversationDao.updateLastActiveTime(conversationId, now)) {
            "Conversation not found: $conversationId"
        }
        return turn
    }

    fun listTurns(
        conversationId: Uuid,
        beforeId: Uuid? = null,
        limit: Int = 50,
    ): List<ConversationTurnPo> {
        return conversationDao.listTurnsByConversationId(conversationId, beforeId, limit)
    }
}
