package life.app.ai.utils

import com.openai.client.OpenAIClient
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseCreateParams

object AiRuns {
    fun runStream(
        client: OpenAIClient,
        params: ResponseCreateParams,
        handler: AiStreamEventHandler = {},
    ): Response {
        var response: Response? = null
        client.responses().createStreaming(params).use { stream ->
            stream.stream().forEach { event ->
                handler(event)
                event.completed().ifPresent { response = it.response() }
            }
        }
        return response ?: error("Response stream completed without completed event")
    }
}
