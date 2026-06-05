package life.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringsTest {
    @Test
    fun `detects line breaks`() {
        assertFalse(Strings.hasLineBreak("hello"))
        assertTrue(Strings.hasLineBreak("hello\nworld"))
        assertTrue(Strings.hasLineBreak("hello\rworld"))
        assertTrue(Strings.hasLineBreak("hello\r\nworld"))
    }

    @Test
    fun `nullable line break check treats null as false`() {
        assertFalse(Strings.hasLineBreak(null))
    }

}
