package life.infra.sse

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import java.nio.charset.StandardCharsets.UTF_8

fun HttpExchange.startSse(
    mapper: ObjectMapper,
): SseWriter {
    responseHeaders.set("Content-Type", "text/event-stream; charset=${UTF_8.name().lowercase()}")
    responseHeaders.set("Cache-Control", "no-cache, no-transform")
    responseHeaders.set("Connection", "keep-alive")
    responseHeaders.set("X-Accel-Buffering", "no")
    sendResponseHeaders(200, 0)
    return SseWriter(responseBody, mapper, UTF_8)
}

fun HttpExchange.sse(
    mapper: ObjectMapper,
    block: SseWriter.() -> Unit,
) {
    startSse(mapper).use { writer -> writer.block() }
}
