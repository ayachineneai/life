package life.util

object Strings {
    fun hasLineBreak(value: String?): Boolean {
        return value?.any { char -> char == '\r' || char == '\n' } == true
    }
}
