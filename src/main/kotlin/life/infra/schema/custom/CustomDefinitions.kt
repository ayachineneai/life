package life.infra.schema.custom

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.victools.jsonschema.generator.CustomDefinition
import com.github.victools.jsonschema.generator.SchemaGenerationContext
import life.infra.jackson.objectNode
import java.time.LocalDateTime

object CustomDefinitions {
    val definitionMap: Map<Class<*>, (SchemaGenerationContext) -> CustomDefinition> = mapOf(
        LocalDateTime::class.java to {
            CustomDefinition(
                localDateTimeSchema(),
                CustomDefinition.DefinitionType.INLINE,
                CustomDefinition.AttributeInclusion.YES,
            )
        },
    )

    private fun localDateTimeSchema(): ObjectNode {
        return objectNode().also { schema ->
            schema.put("type", "string")
            schema.put("pattern", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
        }
    }
}
