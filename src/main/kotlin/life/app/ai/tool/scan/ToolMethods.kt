package life.app.ai.tool.scan

import life.app.ai.tool.Tool
import life.app.ai.tool.annotations.Tool as ToolAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

internal object ToolMethods {
    fun scan(type: KClass<*>): List<Tool> {
        val methods = type.memberFunctions.mapNotNull { function ->
            val annotation = function.findAnnotation<ToolAnnotation>() ?: return@mapNotNull null
            ToolMethod(function, annotation)
        }
        return methods.map { method -> tool(type, method) }
    }

    private fun tool(type: KClass<*>, method: ToolMethod): Tool {
        val name = method.annotation.name.ifBlank { method.function.name }

        return Tool(
            name = name,
            description = method.annotation.description,
            paramsSchema = ParameterSchemas.schema(method.function),
            declaringClass = type,
            method = method.function,
        )
    }

    private data class ToolMethod(
        val function: KFunction<*>,
        val annotation: ToolAnnotation,
    )
}
