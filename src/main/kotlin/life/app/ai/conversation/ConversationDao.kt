package life.app.ai.conversation

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
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

    fun listConversations(limit: Int = 10): List<ConversationPo> {
        return ConversationTable
            .selectAll()
            .orderBy(ConversationTable.lastActiveTime to SortOrder.DESC)
            .limit(limit)
            .map { row -> ConversationMapper.toConversationPo(row) }
    }

    fun updateTitle(id: Uuid, title: String?): Boolean {
        return ConversationTable.update({ ConversationTable.id eq id }) { row ->
            row[ConversationTable.title] = title
        } > 0
    }

    fun updateLastActiveTime(id: Uuid, lastActiveTime: LocalDateTime): Boolean {
        return ConversationTable.update({ ConversationTable.id eq id }) { row ->
            row[ConversationTable.lastActiveTime] = lastActiveTime
        } > 0
    }

    fun insertTurn(turn: ConversationTurnPo): ConversationTurnPo {
        ConversationTurnTable.insert { row ->
            ConversationMapper.fillTurnInsert(row, turn)
        }

        return turn
    }

    fun listTurnsByConversationId(
        conversationId: Uuid,
        beforeId: Uuid? = null,
        limit: Int = 50,
    ): List<ConversationTurnPo> {
        return ConversationTurnTable
            .selectAll()
            .where {
                val conversationCondition = ConversationTurnTable.conversationId eq conversationId

                if (beforeId == null) {
                    conversationCondition
                } else {
                    conversationCondition and (ConversationTurnTable.id less beforeId)
                }
            }
            .orderBy(ConversationTurnTable.id to SortOrder.DESC)
            .limit(limit)
            .map { row -> ConversationMapper.toTurnPo(row) }
            .reversed()
    }
}
