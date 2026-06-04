package life.util

import life.app.ai.tool.annotations.Tool
import life.util.data.Cmd
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

object Bashs {

    fun exec(cmd: Cmd): String {
        return exec("life", "./.life", cmd.command)
    }

    fun exec(user: String, workdir: String, cmd: String): String {
        val process = ProcessBuilder(
            listOf("sudo", "-n", "-u", user, "--", "bash", "-c", cmd)
        )
            .directory(File(workdir))
            .redirectErrorStream(true)
            .start()

        process.outputStream.close()

        val output = try {
            String(process.inputStream.readAllBytes(), UTF_8)
        } finally {
            process.inputStream.close()
        }
        process.waitFor()

        return output
    }

}
