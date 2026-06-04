package life.infra.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

object Stricts {
    fun toStrictFormat(schema: ObjectNode): ObjectNode {
        strict(schema)
        return schema
    }

    private fun strict(node: JsonNode) {
        when (node) {
            is ObjectNode -> {
                node.remove("\$schema")
                node.remove("title")
                additionalProperties(node)
                required(node)
                node.fields().forEach { (_, child) -> strict(child) }
            }
            is ArrayNode -> node.forEach { child -> strict(child) }
        }
    }

    private fun additionalProperties(node: ObjectNode) {
        if (!SchemaNodes.isObjectSchema(node)) {
            return
        }

        node.put("additionalProperties", false)
    }

    private fun required(node: ObjectNode) {
        val properties = node["properties"] as? ObjectNode ?: return
        node.set<ArrayNode>("required", node.arrayNode().apply {
            properties.fieldNames().asSequence().forEach { fieldName -> add(fieldName) }
        })
    }
}
