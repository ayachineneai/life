package life.app.ai.mainloop.support

import life.app.ai.tool.ToolRegistry
import life.app.ai.tool.handler.ToolCallHandler

data class MainLoopTools(
    val registry: ToolRegistry,
    val handler: ToolCallHandler,
)
