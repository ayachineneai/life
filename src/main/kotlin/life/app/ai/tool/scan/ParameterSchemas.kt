package life.app.ai.tool.scan

import com.fasterxml.jackson.databind.node.ObjectNode
import life.infra.schema.SchemaNodes
import life.infra.schema.Nulls
import life.infra.schema.Schemas
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

internal object ParameterSchemas {
    fun schema(function: KFunction<*>): ObjectNode {
        val parameters = function.valueParameters()
        if (parameters.isEmpty()) {
            return SchemaNodes.emptyObjectSchema()
        }

        require(parameters.size == 1) {
            "Tool function ${function.name} must have zero or one parameter."
        }
        val schema = schema(parameters.single().type)
        require(SchemaNodes.isObjectSchema(schema)) {
            "Tool function ${function.name} parameter must be an object type."
        }
        return schema
    }

    private fun schema(type: KType): ObjectNode {
        val schema = Schemas.schema(type.jvmErasure).deepCopy()
        return if (type.isMarkedNullable) {
            Nulls.processNullable(schema)
        } else {
            schema
        }
    }

    private fun KFunction<*>.valueParameters(): List<KParameter> {
        return parameters.filter { parameter -> parameter.kind == KParameter.Kind.VALUE }
    }
}
