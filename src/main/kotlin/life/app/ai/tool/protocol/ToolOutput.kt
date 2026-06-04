package life.app.ai.tool.protocol

import com.openai.models.responses.ResponseInputItem

data class ToolOutput(
    val callId: String,
    val result: ToolResult,
)

internal fun ToolOutput.toResponseInputItem(): ResponseInputItem {
    val output = ResponseInputItem.FunctionCallOutput.builder()
        .callId(callId)
        .outputAsJson(result)
        .build()
    return ResponseInputItem.ofFunctionCallOutput(output)
}
