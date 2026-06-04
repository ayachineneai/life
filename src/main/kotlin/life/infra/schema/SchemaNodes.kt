package life.infra.schema

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import life.infra.jackson.arrayNode
import life.infra.jackson.objectNode

object SchemaNodes {
    fun emptyObjectSchema(): ObjectNode {
        return objectNode().also { schema ->
            schema.put("type", "object")
            schema.set<ObjectNode>("properties", objectNode())
            schema.set<ArrayNode>("required", arrayNode())
            schema.put("additionalProperties", false)
        }
    }

    fun isObjectSchema(node: ObjectNode): Boolean {
        val type = node["type"] ?: return false

        val hasObjectType = when (type) {
            is ArrayNode -> type.any { item -> item.asText() == "object" }
            else -> type.asText() == "object"
        }

        return hasObjectType && node["properties"] is ObjectNode
    }
}
