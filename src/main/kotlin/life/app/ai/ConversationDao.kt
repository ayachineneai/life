package life.app.ai

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime
import kotlin.uuid.Uuid

class ConversationDao {
    fun insertConversation(conversation: ConversationPo): ConversationPo {
        ConversationTable.insert { row ->
            ConversationMapper.fillConversationInsert(row, conversation)
        }

        return conversation
    }

    fun findConversationById(id: Uuid): ConversationPo? {
        return ConversationTable
            .selectAll()
            .where { ConversationTable.id eq id }
            .singleOrNull()
            ?.let { row -> ConversationMapper.toConversationPo(row) }
    }

    fun listConversations(): List<ConversationPo> {
        return ConversationTable
            .selectAll()
            .orderBy(ConversationTable.lastActiveTime to SortOrder.DESC)
            .map { row -> ConversationMapper.toConversationPo(row) }
    }

    fun updateTitle(id: Uuid, title: String?): Boolean {
        val affectedRows = ConversationTable.update({ ConversationTable.id eq id }) { row ->
            ConversationMapper.fillConversationTitleUpdate(row, title)
        }

        return affectedRows > 0
    }

    fun updateLastActiveTime(id: Uuid, lastActiveTime: LocalDateTime): Boolean {
        val affectedRows = ConversationTable.update({ ConversationTable.id eq id }) { row ->
            ConversationMapper.fillConversationLastActiveTimeUpdate(row, lastActiveTime)
        }

        return affectedRows > 0
    }

    fun insertTurn(turn: ConversationTurnPo): ConversationTurnPo {
        ConversationTurnTable.insert { row ->
            ConversationMapper.fillTurnInsert(row, turn)
        }

        return turn
    }

    fun listTurnsByConversationId(conversationId: Uuid): List<ConversationTurnPo> {
        return ConversationTurnTable
            .selectAll()
            .where { ConversationTurnTable.conversationId eq conversationId }
            .orderBy(ConversationTurnTable.createTime to SortOrder.ASC)
            .map { row -> ConversationMapper.toTurnPo(row) }
    }
}
