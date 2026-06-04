package life.infra.schema

import com.fasterxml.jackson.annotation.JsonValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SchemasTest {
    @Test
    fun `prints generated schema`() {
        println(Schemas.schema(ToolArgs::class).toPrettyString())
    }

    @Test
    fun `generates openai strict parameters from class`() {
        val schema = Schemas.schema(ToolArgs::class)
        val properties = schema["properties"]

        assertNotNull(properties)
        assertNotNull(properties["command"])
        assertEquals(false, schema["additionalProperties"].booleanValue())
        assertEquals(
            setOf("command", "timeoutSeconds", "mealType", "ownerId", "traceId"),
            schema["required"].map { it.textValue() }.toSet(),
        )
    }

    @Test
    fun `generates enum as openai enum`() {
        val mealType = Schemas.schema(ToolArgs::class)["properties"]["mealType"]

        assertEquals("string", mealType["type"].textValue())
        assertEquals(listOf("BREAKFAST", "LUNCH"), mealType["enum"].map { it.textValue() })
    }

    @Test
    fun `generates value object as its json value type`() {
        val properties = Schemas.schema(ToolArgs::class)["properties"]

        assertEquals("string", properties["ownerId"]["type"].textValue())
        assertEquals(listOf("string", "null"), properties["traceId"]["type"].map { it.textValue() })
    }

    data class ToolArgs(
        val command: String,
        val timeoutSeconds: Int?,
        val mealType: MealType,
        val ownerId: UserId,
        val traceId: TraceId?,
    )

    enum class MealType {
        BREAKFAST,
        LUNCH,
    }

    data class UserId(
        @get:JsonValue val value: String,
    )

    @JvmInline
    value class TraceId(
        val value: String,
    )
}
