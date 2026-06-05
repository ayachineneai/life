package life.infra.sse

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import java.io.Closeable
import java.io.Flushable
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

class SseWriter(
    private val output: OutputStream,
    private val charset: Charset = UTF_8,
) : Closeable, Flushable {
    @Synchronized
    fun send(event: SseEvent) {
        output.write(event.encode().toByteArray(charset))
        output.flush()
    }

    fun send(data: String) {
        send(SseEvent.message(data))
    }

    fun send(event: String, data: String, id: String? = null, retry: Long? = null) {
        send(SseEvent.named(event = event, data = data, id = id, retry = retry))
    }

    fun sendJson(
        event: String,
        data: Any?,
        id: String? = null,
        retry: Long? = null,
        mapper: ObjectMapper
    ) {
        send(
            event = SseEvent.named(
                event = event,
                data = mapper.writeValueAsString(data),
                id = id,
                retry = retry,
            )
        )
    }

    fun comment(comment: String = "") {
        send(SseEvent.comment(comment))
    }

    fun retry(retry: Long) {
        send(SseEvent.retry(retry))
    }

    override fun flush() {
        output.flush()
    }

    override fun close() {
        output.close()
    }
}
