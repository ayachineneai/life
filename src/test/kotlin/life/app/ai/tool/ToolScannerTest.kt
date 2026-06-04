package life.app.ai.tool

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.openai.models.responses.FunctionTool
import life.app.ai.tool.fixtures.valid.FileTools
import life.app.ai.tool.fixtures.valid.WeatherTools
import life.app.ai.tool.scan.ToolScanner
import life.infra.jackson.objectNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ToolScannerTest {
    @Test
    fun `scans single pojo parameter as tool parameters`() {
        val tool = ToolScanner.scan("life.app.ai.tool.fixtures.valid").require("get_weather")
        val functionTool = tool.toFunctionCall().asFunction()
        val parameters = functionTool.parameters().orElseThrow()
        val properties = tool.paramsSchema["properties"]

        assertEquals("get_weather", tool.name)
        assertEquals("Gets weather by city.", tool.description)
        assertEquals(WeatherTools::class, tool.declaringClass)
        assertEquals("weather", tool.method.name)
        assertEquals("get_weather", functionTool.name())
        assertEquals("object", tool.paramsSchema["type"].textValue())
        assertEquals("string", properties["city"]["type"].textValue())
        assertEquals(listOf("city", "unit"), tool.paramsSchema["required"].map { it.textValue() })
        assertEquals(tool.paramsSchema.toString(), parameters.json().toString())
    }

    @Test
    fun `scans single object parameter for file tool`() {
        val tool = ToolScanner.scan("life.app.ai.tool.fixtures.valid").require("list_files")
        val properties = tool.paramsSchema["properties"]

        assertEquals("list_files", tool.name)
        assertEquals(FileTools::class, tool.declaringClass)
        assertEquals("list", tool.method.name)
        assertEquals("object", tool.paramsSchema["type"].textValue())
        assertEquals("string", properties["path"]["type"].textValue())
        assertNull(properties["path"]["additionalProperties"])
        assertEquals("boolean", properties["recursive"]["type"].textValue())
        assertEquals(listOf("integer", "null"), properties["limit"]["type"].map { it.textValue() })
        assertEquals(setOf("path", "recursive", "limit"), tool.paramsSchema["required"].map { it.textValue() }.toSet())
    }

    @Test
    fun `builds registry definitions`() {
        val registry = ToolScanner.scan("life.app.ai.tool.fixtures.valid")

        assertEquals(2, registry.tools.size)
        assertEquals("get_weather", registry.require("get_weather").name)
        assertEquals(listOf("get_weather", "list_files"), registry.openAiTools().map { it.asFunction().name() })
    }

    @Test
    fun `scans annotated tools from base package`() {
        val tool = ToolScanner.scan("life.util").require("exec")
        val properties = tool.paramsSchema["properties"]

        assertEquals("exec", tool.name)
        assertEquals("object", tool.paramsSchema["type"].textValue())
        assertEquals("string", properties["command"]["type"].textValue())
    }

    @Test
    fun `rejects tool methods with multiple parameters`() {
        assertFailsWith<IllegalArgumentException> {
            ToolScanner.scan("life.app.ai.tool.fixtures.invalid")
        }
    }

    private fun FunctionTool.Parameters.json(): ObjectNode {
        return objectNode().also { node ->
            _additionalProperties().forEach { (name, value) ->
                node.set<JsonNode>(name, value.convert(JsonNode::class.java))
            }
        }
    }
}
