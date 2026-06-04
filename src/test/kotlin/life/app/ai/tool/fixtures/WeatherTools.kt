package life.app.ai.tool.fixtures.valid

import life.app.ai.tool.annotations.Tool

object WeatherTools {
    @Tool(name = "get_weather", description = "Gets weather by city.")
    fun weather(args: WeatherArgs): String {
        return "${args.city}:${args.unit}"
    }
}

data class WeatherArgs(
    val city: String,
    val unit: String,
)
