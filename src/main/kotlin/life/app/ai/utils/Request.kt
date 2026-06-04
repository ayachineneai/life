package life.app.ai.utils

import com.openai.models.ChatModel
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.Tool

object AiRequests {
    val chatModel: ChatModel = ChatModel.GPT_5_4_MINI

    fun text(
        text: String,
        model: ChatModel = chatModel,
        systemPrompt: String = "",
        tools: List<Tool> = emptyList(),
    ): ResponseCreateParams {
        return ResponseCreateParams.builder()
            .model(model)
            .input(text)
            .apply {
                if (systemPrompt.isNotBlank()) {
                    instructions(systemPrompt)
                }
                tools.forEach { tool -> addTool(tool) }
            }
            .build()
    }

    fun conversationText(
        conversationId: String,
        text: String,
        model: ChatModel = chatModel,
        systemPrompt: String = "",
        tools: List<Tool> = emptyList(),
    ): ResponseCreateParams {
        return text(
            text = text,
            model = model,
            systemPrompt = systemPrompt,
            tools = tools,
        )
            .toBuilder()
            .conversation(conversationId)
            .build()
    }

    fun responseText(
        text: String,
        previousResponseId: String? = null,
        model: ChatModel = chatModel,
        systemPrompt: String = "",
        tools: List<Tool> = emptyList(),
    ): ResponseCreateParams {
        return text(
            text = text,
            model = model,
            systemPrompt = systemPrompt,
            tools = tools,
        )
            .toBuilder()
            .apply {
                if (previousResponseId != null) {
                    previousResponseId(previousResponseId)
                }
            }
            .build()
    }
}
