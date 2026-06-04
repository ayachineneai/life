package life.app.ai.utils

import com.openai.models.responses.Response
import com.openai.models.responses.ResponseFunctionToolCall
import com.openai.models.responses.ResponseOutputItem
import java.util.Optional

fun Response.conversationId(): String? {
    return conversation()
        .map { conversation -> conversation.id() }
        .orElse(null)
}

fun Response.outputMessages() = outputItems { item -> item.message() }

fun Response.outputContents() = outputMessages()
    .asSequence()
    .flatMap { message -> message.content().asSequence() }
    .toList()

fun Response.outputTexts(): List<String> {
    return outputContents()
        .asSequence()
        .mapNotNull { content -> content.outputText().orElse(null) }
        .map { outputText -> outputText.text() }
        .toList()
}

fun Response.outputText(separator: String = ""): String {
    return outputTexts().joinToString(separator)
}

fun Response.firstOutputText(): String? {
    return outputTexts().firstOrNull()
}

fun Response.refusals(): List<String> {
    return outputContents()
        .asSequence()
        .mapNotNull { content -> content.refusal().orElse(null) }
        .map { refusal -> refusal.refusal() }
        .toList()
}

fun Response.refusal(): String? {
    return refusals().firstOrNull()
}

fun Response.hasRefusal(): Boolean {
    return refusals().isNotEmpty()
}

fun Response.functionCalls(): List<ResponseFunctionToolCall> {
    return outputItems { item -> item.functionCall() }
}

fun Response.hasToolCall(): Boolean {
    return functionCalls().isNotEmpty()
}

fun Response.reasoningItems() = outputItems { item -> item.reasoning() }

fun Response.additionalTools() = outputItems { item -> item.additionalTools() }

fun Response.compactions() = outputItems { item -> item.compaction() }

private fun <T : Any> Response.outputItems(extract: (ResponseOutputItem) -> Optional<T>): List<T> {
    return output()
        .asSequence()
        .mapNotNull { item -> extract(item).orElse(null) }
        .toList()
}
