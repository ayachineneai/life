package life.app.ai.http

import life.app.ai.mainloop.support.Event
import life.infra.sse.SseWriter

fun SseWriter.sendAgentEvent(event: Event) {
    sendJson(event, Event::class.java)
}
