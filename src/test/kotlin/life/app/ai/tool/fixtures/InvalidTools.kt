package life.app.ai.tool.fixtures.invalid

import life.app.ai.tool.annotations.Tool

object InvalidTools {
    @Tool(name = "invalid_multiple_params")
    fun invalid(first: String, second: String): String {
        return "$first:$second"
    }
}
