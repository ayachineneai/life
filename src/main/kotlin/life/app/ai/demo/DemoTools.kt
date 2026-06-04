package life.app.ai.demo

import life.app.ai.tool.annotations.Tool
import life.infra.schema.PropDesc

object DemoTools {
    @Tool(
        name = "demo_echo",
        description = "Returns the provided message. Use this tool in the streaming demo to verify tool calls.",
    )
    fun echo(args: DemoEchoArgs): String {
        return "demo_echo: ${args.message}"
    }
}

data class DemoEchoArgs(
    @get:PropDesc("The message to echo back.")
    val message: String,
)
