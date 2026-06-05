package life.infra.sse

import com.sun.net.httpserver.HttpExchange
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

fun HttpExchange.startSse(
): SseWriter {
    responseHeaders.set("Content-Type", "text/event-stream; charset=${UTF_8.name().lowercase()}")
    responseHeaders.set("Cache-Control", "no-cache, no-transform")
    responseHeaders.set("Connection", "keep-alive")
    responseHeaders.set("X-Accel-Buffering", "no")
    sendResponseHeaders(200, 0)
    return SseWriter(responseBody, UTF_8)
}

fun HttpExchange.sse(
    block: SseWriter.() -> Unit,
) {
    startSse().use { writer -> writer.block() }
}
