package life.app.ai.conversation

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder

object ConversationMapper {
    fun fillConversationInsert(row: UpdateBuilder<*>, conversation: ConversationPo) {
        row[ConversationTable.openaiConversationId] = conversation.openaiConversationId
        row[ConversationTable.title] = conversation.title
        row[ConversationTable.id] = conversation.id
        row[ConversationTable.createTime] = conversation.createTime
        row[ConversationTable.lastActiveTime] = conversation.lastActiveTime
    }

    fun fillTurnInsert(row: UpdateBuilder<*>, turn: ConversationTurnPo) {
        row[ConversationTurnTable.id] = turn.id
        row[ConversationTurnTable.conversationId] = turn.conversationId
        row[ConversationTurnTable.userInput] = turn.userInput
        row[ConversationTurnTable.modelOutput] = turn.modelOutput
        row[ConversationTurnTable.openaiResponseId] = turn.openaiResponseId
        row[ConversationTurnTable.model] = turn.model
        row[ConversationTurnTable.createTime] = turn.createTime
    }

    fun toConversationPo(row: ResultRow): ConversationPo {
        return ConversationPo(
            id = row[ConversationTable.id],
            openaiConversationId = row[ConversationTable.openaiConversationId],
            title = row[ConversationTable.title],
            createTime = row[ConversationTable.createTime],
            lastActiveTime = row[ConversationTable.lastActiveTime],
        )
    }

    fun toTurnPo(row: ResultRow): ConversationTurnPo {
        return ConversationTurnPo(
            id = row[ConversationTurnTable.id],
            conversationId = row[ConversationTurnTable.conversationId],
            userInput = row[ConversationTurnTable.userInput],
            modelOutput = row[ConversationTurnTable.modelOutput],
            openaiResponseId = row[ConversationTurnTable.openaiResponseId],
            model = row[ConversationTurnTable.model],
            createTime = row[ConversationTurnTable.createTime],
        )
    }
}
