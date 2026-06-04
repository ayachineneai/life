package life.infra.jackson

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

fun objectNode(): ObjectNode {
    return JsonNodeFactory.instance.objectNode()
}

fun arrayNode(): ArrayNode {
    return JsonNodeFactory.instance.arrayNode()
}
