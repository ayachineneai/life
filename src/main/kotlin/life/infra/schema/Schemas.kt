package life.infra.schema

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import com.github.victools.jsonschema.module.jackson.JacksonModule
import com.github.victools.jsonschema.module.jackson.JacksonOption
import com.openai.core.JsonValue
import com.openai.models.responses.FunctionTool
import life.infra.schema.custom.CustomDefinitions
import kotlin.reflect.KClass

object Schemas {
    private val mapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    private val generator = SchemaGenerator(
        SchemaGeneratorConfigBuilder(
            mapper,
            SchemaVersion.DRAFT_2020_12,
            OptionPreset.PLAIN_JSON)
            .apply { forTypesInGeneral().withCustomDefinitionProvider(ValueProvider(CustomDefinitions.definitionMap)) }
            .with(
                JacksonModule(
                    JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
                    JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE,
                    JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY,
                )
            ).with(
                Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT,
                Option.FLATTENED_ENUMS,
                Option.INLINE_ALL_SCHEMAS,
            ).build()
    )

    fun schema(type: KClass<*>): ObjectNode {
        return Nulls.processNullable(schema(type.java), type)
    }

    fun schema(type: Class<*>): ObjectNode {
        return Stricts.toStrictFormat(generator.generateSchema(type))
    }

    fun parameters(schema: ObjectNode): FunctionTool.Parameters {
        val builder = FunctionTool.Parameters.builder()
        schema.fields().asSequence().forEach { field ->
            builder.putAdditionalProperty(field.key, JsonValue.fromJsonNode(field.value))
        }
        return builder.build()
    }
}
