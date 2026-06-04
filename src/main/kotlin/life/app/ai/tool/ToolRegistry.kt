package life.app.ai.tool

import com.openai.models.responses.Tool as OpenAiTool

class ToolRegistry(tools: List<Tool>) {
    val tools: List<Tool> = tools.sortedBy { tool -> tool.name }
    private val toolsByName: Map<String, Tool> = this.tools.associateBy { tool -> tool.name }

    init {
        val duplicatedNames = tools.groupBy { tool -> tool.name }
            .filterValues { group -> group.size > 1 }
            .keys

        require(duplicatedNames.isEmpty()) {
            "Duplicate AI tool names: ${duplicatedNames.joinToString()}"
        }
    }

    fun get(name: String): Tool? {
        return toolsByName[name]
    }

    fun require(name: String): Tool {
        return get(name) ?: error("AI tool not found: $name")
    }

    fun definitions(): List<OpenAiTool> {
        return tools.map { tool -> tool.toFunctionCall() }
    }
}
