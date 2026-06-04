package life.app.ai.tool.scan

import life.app.ai.tool.ToolRegistry
import com.openai.models.responses.Tool as OpenAiTool

object ToolScanner {
    fun tools(
        basePackage: String,
        classLoader: ClassLoader = ToolClasses.defaultClassLoader(),
    ): List<OpenAiTool> {
        return scan(basePackage, classLoader).definitions()
    }

    fun scan(
        basePackage: String,
        classLoader: ClassLoader = ToolClasses.defaultClassLoader(),
    ): ToolRegistry {
        return ToolRegistry(ToolClasses.find(basePackage, classLoader).flatMap { type ->
            ToolMethods.scan(type)
        })
    }
}
