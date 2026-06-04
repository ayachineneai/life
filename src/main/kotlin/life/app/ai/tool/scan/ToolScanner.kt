package life.app.ai.tool.scan

import life.app.ai.tool.ToolRegistry
import com.openai.models.responses.Tool as OpenAiTool

object ToolScanner {
    fun scan(
        basePackage: String,
        classLoader: ClassLoader = ToolClasses.defaultClassLoader(),
    ): ToolRegistry {
        return ToolRegistry(ToolClasses.find(basePackage, classLoader).flatMap { type ->
            ToolMethods.scan(type)
        })
    }
}
