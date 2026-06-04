package life.app.ai.tool.handler

import com.fasterxml.jackson.databind.json.JsonMapper
import com.openai.models.responses.ResponseFunctionToolCall
import com.openai.models.responses.ResponseOutputItem as OpenAiToolCall
import life.app.ai.tool.fixtures.valid.FileTools
import life.app.ai.tool.scan.ToolScanner
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ToolCallHandlerTest {
    private val mapper = JsonMapper.builder().findAndAddModules().build()

    @AfterTest
    fun stopGlobalKoin() {
        GlobalContext.getOrNull()?.let { stopKoin() }
    }

    @Test
    fun `invokes class tool through koin`() {
        val koin = startKoin {
            modules(module { single { FileTools() } })
        }.koin
        val registry = ToolScanner.scan("life.app.ai.tool.fixtures.valid")
        val handler = ToolCallHandler(registry, koin)

        val result = handler.handle(functionCall(
            name = "list_files",
            arguments = """{"path":"src","recursive":true,"limit":3}""",
        )) ?: error("Expected function tool call output")

        val output = result.asFunctionCallOutput()
        val body = mapper.readTree(output.output().asString())

        assertEquals("call_1", output.callId())
        assertEquals(true, body["ok"].booleanValue())
        assertEquals("list_files", body["tool"].textValue())
        assertEquals("src:true:3", body["result"].textValue())
    }

    @Test
    fun `returns tool error envelope`() {
        val koin = startKoin {
            modules(module { single { FileTools() } })
        }.koin
        val registry = ToolScanner.scan("life.app.ai.tool.fixtures.valid")
        val handler = ToolCallHandler(registry, koin)

        val result = handler.handle(functionCall(
            name = "missing_tool",
            arguments = """{}""",
        )) ?: error("Expected function tool call output")

        val body = mapper.readTree(result.asFunctionCallOutput().output().asString())
        assertEquals(false, body["ok"].booleanValue())
        assertEquals("missing_tool", body["tool"].textValue())
        assertEquals("tool_execution_error", body["error"]["code"].textValue())
    }

    private fun functionCall(name: String, arguments: String): OpenAiToolCall {
        return OpenAiToolCall.ofFunctionCall(
            ResponseFunctionToolCall.builder()
                .callId("call_1")
                .name(name)
                .arguments(arguments)
                .build()
        )
    }
}
