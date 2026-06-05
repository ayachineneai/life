package life.infra.sse

object SseEncoder {
    fun encode(event: SseEvent): String {
        return buildString {
            event.comment?.let { comment -> appendComment(comment) }
            event.id?.let { id -> appendField("id", id) }
            event.event?.let { name -> appendField("event", name) }
            event.retry?.let { retry -> appendField("retry", retry.toString()) }
            event.data?.let { data -> appendData(data) }
            append('\n')
        }
    }

    private fun StringBuilder.appendField(name: String, value: String) {
        append(name)
        append(": ")
        append(value)
        append('\n')
    }

    private fun StringBuilder.appendComment(comment: String) {
        splitLines(comment).forEach { line ->
            append(':')
            if (line.isNotEmpty()) {
                append(' ')
                append(line)
            }
            append('\n')
        }
    }

    private fun StringBuilder.appendData(data: String) {
        splitLines(data).forEach { line -> appendField("data", line) }
    }

    private fun splitLines(value: String): List<String> {
        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
    }
}
