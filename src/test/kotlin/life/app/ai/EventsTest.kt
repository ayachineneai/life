package life.app.ai

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import life.app.ai.mainloop.support.AgentStreamEvents
import life.app.ai.mainloop.support.Event
import life.app.ai.mainloop.support.ToolState
import life.app.ai.mainloop.support.TurnPhase
import life.app.ai.mainloop.support.TurnStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class EventsTest {
    private val mapper = JsonMapper.builder().findAndAddModules().build()

    @Test
    fun `serializes event type from concrete event class`() {
        val json = mapper.writerFor(Event::class.java)
            .writeValueAsString(AgentStreamEvents.status(TurnStatus.THINKING))
        val node: JsonNode = mapper.readTree(json)

        assertEquals("STATUS", node["type"].asText())
        assertEquals("THINKING", node["status"].asText())
    }

    @Test
    fun `serializes tool event fields without nested payload`() {
        val json = mapper.writerFor(Event::class.java)
            .writeValueAsString(
                AgentStreamEvents.tool(
                    phase = TurnPhase.ACT,
                    state = ToolState.EXECUTING,
                    callId = "call_1",
                    name = "record_meal",
                )
            )
        val node: JsonNode = mapper.readTree(json)

        assertEquals("TOOL", node["type"].asText())
        assertEquals("act", node["phase"].asText())
        assertEquals("executing", node["state"].asText())
        assertEquals("call_1", node["callId"].asText())
        assertEquals("record_meal", node["name"].asText())
    }

    @Test
    fun `serializes plan delta event`() {
        val json = mapper.writerFor(Event::class.java)
            .writeValueAsString(AgentStreamEvents.planDelta("""{"summary":"""))
        val node: JsonNode = mapper.readTree(json)

        assertEquals("PLAN_DELTA", node["type"].asText())
        assertEquals("""{"summary":""", node["text"].asText())
    }
}
