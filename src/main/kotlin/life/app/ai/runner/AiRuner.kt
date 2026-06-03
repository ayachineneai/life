package life.app.ai.runner

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.Response
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.annotation.JsonTypeName

class AiRuner(
    private val client: OpenAIClient = OpenAIOkHttpClient.fromEnv(),
) {
    fun test(): Response {
        val params = ResponseCreateParams.builder()
            .model("gpt-5.2")
            .input("List files in the current workspace.")
            .addTool(RunBash::class.java)
            .build()

        return client.responses().create(params)
    }
}

@JsonTypeName("run_bash")
@JsonClassDescription("Execute a bash command in the configured AI workspace.")
class RunBash {
    @get:JsonPropertyDescription("The bash command to execute.")
    var cmd: String = ""
}
