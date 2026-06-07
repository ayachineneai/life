package life.app.ai.tool.handler

import com.openai.models.responses.ResponseFunctionToolCall
import com.openai.models.responses.ResponseOutputItem as OpenAiToolCall
import life.app.ai.tool.Tool
import life.app.ai.tool.ToolRegistry
import life.app.ai.tool.protocol.ToolError
import life.app.ai.tool.protocol.ToolOutput
import life.app.ai.tool.protocol.ToolResult
import org.koin.core.Koin
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

class ToolCallHandler(
    private val registry: ToolRegistry,
    private val koin: Koin,
) {
    fun handle(call: OpenAiToolCall): ToolOutput? {
        val functionCall = call.functionCall()
        return when {
            functionCall.isPresent -> handle(functionCall.get())
            else -> null
        }
    }

    private fun handle(functionCall: ResponseFunctionToolCall): ToolOutput {
        val result = runCatching {
            val tool = registry.require(functionCall.name())
            val args = arguments(tool, functionCall)
            ToolResult.success(tool.name, normalize(invoke(tool, args)))
        }.getOrElse { error ->
            error.printStackTrace()
            ToolResult.failure(functionCall.name(), toolError(error))
        }
        return output(functionCall.callId(), result)
    }

    private fun arguments(tool: Tool, call: ResponseFunctionToolCall): List<Any?> {
        return tool.method.valueParameters.map { parameter ->
            call.arguments(parameter.type.jvmErasure.java)
        }
    }

    private fun invoke(tool: Tool, args: List<Any?>): Any? {
        val method = tool.method
        val instance = method.instanceParameter?.let { instance(tool) }
        return if (instance == null) {
            method.call(*args.toTypedArray())
        } else {
            method.call(instance, *args.toTypedArray())
        }
    }

    private fun instance(tool: Tool): Any {
        return tool.declaringClass.objectInstance ?: koin.get(clazz = tool.declaringClass)
    }

    private fun normalize(result: Any?): Any? {
        return when (result) {
            Unit -> null
            else -> result
        }
    }

    private fun toolError(error: Throwable): ToolError {
        val cause = (error as? InvocationTargetException)?.targetException ?: error
        val message = cause.message ?: cause::class.qualifiedName ?: "Tool call failed"
        return ToolError(code = "tool_execution_error", message = message)
    }

    private fun output(callId: String, result: ToolResult): ToolOutput {
        return ToolOutput(callId = callId, result = result)
    }
}
