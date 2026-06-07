package life.app.ai.mainloop.support

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ConversationEvent::class, name = "CONVERSATION"),
    JsonSubTypes.Type(value = StatusEvent::class, name = "STATUS"),
    JsonSubTypes.Type(value = PlanDeltaEvent::class, name = "PLAN_DELTA"),
    JsonSubTypes.Type(value = DeltaEvent::class, name = "DELTA"),
    JsonSubTypes.Type(value = ToolEvent::class, name = "TOOL"),
    JsonSubTypes.Type(value = DoneEvent::class, name = "DONE"),
    JsonSubTypes.Type(value = ErrorEvent::class, name = "ERROR"),
)
sealed interface Event

enum class TurnStatus {
    PREPARING,
    THINKING,
    USING_TOOL,
    RESPONDING,
    COMPLETED,
    FAILED,
}

enum class TurnPhase(private val wireName: String) {
    ACT("act"),
    ;

    @JsonValue
    fun jsonValue(): String {
        return wireName
    }
}

enum class ToolState(private val wireName: String) {
    EXECUTING("executing"),
    MISSING_TOOL("missing_tool"),
    RESULT("result"),
    ;

    @JsonValue
    fun jsonValue(): String {
        return wireName
    }
}

data class ConversationEvent(
    val conversationId: String,
) : Event

data class StatusEvent(
    val status: TurnStatus,
) : Event

data class PlanDeltaEvent(
    val text: String,
) : Event

data class DeltaEvent(
    val text: String,
) : Event

data class ToolEvent(
    val phase: TurnPhase,
    val state: ToolState,
    val callId: String? = null,
    val name: String? = null,
    val arguments: String? = null,
    val result: Any? = null,
) : Event

data class DoneEvent(
    val responseId: String?,
) : Event

data class ErrorEvent(
    val error: String,
) : Event

object AgentStreamEvents {
    fun conversation(conversationId: String): Event {
        return ConversationEvent(
            conversationId = conversationId,
        )
    }

    fun status(status: TurnStatus): Event {
        return StatusEvent(status)
    }

    fun planDelta(text: String): Event {
        return PlanDeltaEvent(text)
    }

    fun delta(text: String): Event {
        return DeltaEvent(text)
    }

    fun tool(
        phase: TurnPhase,
        state: ToolState,
        callId: String? = null,
        name: String? = null,
        arguments: String? = null,
        result: Any? = null,
    ): Event {
        return ToolEvent(
            phase = phase,
            state = state,
            callId = callId,
            name = name,
            arguments = arguments,
            result = result,
        )
    }

    fun done(responseId: String?): Event {
        return DoneEvent(responseId)
    }

    fun error(message: String): Event {
        return ErrorEvent(message)
    }
}
