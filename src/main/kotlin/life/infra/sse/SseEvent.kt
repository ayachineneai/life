package life.infra.sse

import life.util.Strings

data class SseEvent(
    val data: String? = null,
    val event: String? = null,
    val id: String? = null,
    val retry: Long? = null,
    val comment: String? = null,
) {
    init {
        require(!Strings.hasLineBreak(event)) { "SSE event must not contain line breaks" }
        require(!Strings.hasLineBreak(id)) { "SSE id must not contain line breaks" }
        require(retry == null || retry >= 0) { "SSE retry must be greater than or equal to 0" }
    }

    fun encode(): String {
        return SseEncoder.encode(this)
    }

    companion object {
        fun message(
            data: String,
            id: String? = null,
            retry: Long? = null,
        ): SseEvent {
            return SseEvent(data = data, id = id, retry = retry)
        }

        fun named(
            event: String,
            data: String,
            id: String? = null,
            retry: Long? = null,
        ): SseEvent {
            return SseEvent(event = event, data = data, id = id, retry = retry)
        }

        fun comment(comment: String = ""): SseEvent {
            return SseEvent(comment = comment)
        }

        fun retry(retry: Long): SseEvent {
            return SseEvent(retry = retry)
        }
    }
}
