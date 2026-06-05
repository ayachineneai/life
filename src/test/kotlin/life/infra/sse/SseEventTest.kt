package life.infra.sse

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SseEventTest {
    @Test
    fun `encodes named multiline event`() {
        val encoded = SseEvent.named(
            event = "chat.delta",
            id = "event_1",
            retry = 1500,
            data = "hello\nworld",
        ).encode()

        assertEquals("id: event_1\nevent: chat.delta\nretry: 1500\ndata: hello\ndata: world\n\n", encoded)
    }

    @Test
    fun `encodes comments and blank data`() {
        assertEquals(": ping\n\n", SseEvent.comment("ping").encode())
        assertEquals("data: \n\n", SseEvent.message("").encode())
    }

    @Test
    fun `encodes retry control frame`() {
        assertEquals("retry: 3000\n\n", SseEvent.retry(3000).encode())
    }

    @Test
    fun `rejects invalid fields`() {
        assertFailsWith<IllegalArgumentException> {
            SseEvent.named(event = "bad\nevent", data = "hello")
        }
        assertFailsWith<IllegalArgumentException> {
            SseEvent.message(data = "hello", retry = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            SseEvent.retry(-1)
        }
    }

    @Test
    fun `writer sends and flushes events`() {
        val output = ByteArrayOutputStream()
        val writer = SseWriter(output)

        writer.send("chat.delta", """{"text":"hi"}""")
        writer.comment("done")

        assertEquals("event: chat.delta\ndata: {\"text\":\"hi\"}\n\n: done\n\n", output.toString(Charsets.UTF_8))
    }
}
