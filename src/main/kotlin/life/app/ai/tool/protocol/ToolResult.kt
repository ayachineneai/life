package life.app.ai.tool.protocol

data class ToolResult(
    val ok: Boolean,
    val tool: String,
    val result: Any? = null,
    val error: ToolError? = null,
) {
    companion object {
        fun success(tool: String, result: Any?): ToolResult {
            return ToolResult(ok = true, tool = tool, result = result)
        }

        fun failure(tool: String, error: ToolError): ToolResult {
            return ToolResult(ok = false, tool = tool, error = error)
        }
    }
}
