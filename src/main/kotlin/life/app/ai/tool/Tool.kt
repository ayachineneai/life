package life.app.ai.tool

import com.fasterxml.jackson.databind.node.ObjectNode
import com.openai.models.responses.FunctionTool
import life.infra.schema.Schemas
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import com.openai.models.responses.Tool as OpenAiTool

data class Tool(
    val name: String,
    val description: String,
    val paramsSchema: ObjectNode,
    val declaringClass: KClass<*>,
    val method: KFunction<*>,
) {
    fun toFunctionCall(): OpenAiTool {
        return OpenAiTool.ofFunction(toFunctionTool())
    }

    fun toTool(): OpenAiTool {
        return toFunctionCall()
    }

    fun toFunctionTool(): FunctionTool {
        return FunctionTool.builder()
            .name(name)
            .parameters(Schemas.parameters(paramsSchema))
            .strict(true)
            .apply {
                if (description.isNotBlank()) {
                    description(description)
                }
            }
            .build()
    }
}
