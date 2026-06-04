package life.infra.schema

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmErasure

object Nulls {
    fun processNullable(schema: ObjectNode): ObjectNode {
        schema.markNullable()
        return schema
    }

    fun processNullable(schema: ObjectNode, type: KClass<*>): ObjectNode {
        val properties = schema["properties"] as? ObjectNode ?: return schema

        type.memberProperties.forEach { p ->
            val s = properties[p.propertyName()] as? ObjectNode ?: return@forEach
            if (p.returnType.isMarkedNullable) {
                s.markNullable()
            }

            processNullable(s, p.returnType.jvmErasure)
        }
        return schema
    }

    private fun KProperty1<*, *>.propertyName(): String {
        return jsonPropertyName() ?: name
    }

    private fun KProperty1<*, *>.jsonPropertyName(): String? {
        return javaGetter?.jsonPropertyName()
            ?: javaField?.jsonPropertyName()
    }

    private fun java.lang.reflect.AnnotatedElement.jsonPropertyName(): String? {
        return getAnnotation(JsonProperty::class.java)
            ?.value
            ?.takeIf { name -> name.isNotBlank() }
    }

    private fun ObjectNode.markNullable() {
        val type = this["type"] ?: return
        when (type) {
            is ArrayNode -> type.addNullTypeIfMissing()
            else -> set<ArrayNode>("type", arrayNode().add(type.asText()).apply { addNullTypeIfMissing() })
        }
    }

    private fun ArrayNode.addNullTypeIfMissing() {
        if (none { item -> item.asText() == "null" }) {
            add("null")
        }
    }
}
