package life.util

import io.github.cdimascio.dotenv.Dotenv

fun Dotenv.required(name: String): String {
    return optional(name)
        ?: error("Missing required environment variable: $name")
}

fun Dotenv.optional(name: String): String? {
    return this[name]?.takeIf { it.isNotBlank() }
}
