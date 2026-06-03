package life.app.ai.tool

import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

class BashTool(
    private val user: String,
    private val workDir: File,
    private val timeout: Duration = Duration.ofSeconds(60),
) {
    fun execute(cmd: String): String {
        val process = startCommand(cmd)
        val output = StringBuilder()
        val reader = readOutputInBackground(process, output)

        waitForCommand(process, reader)
        checkCommandResult(process, output)

        return output.toString()
    }

    private fun startCommand(cmd: String): Process {
        return ProcessBuilder(cmdWithUser(cmd))
            .directory(workDir)
            .redirectErrorStream(true)
            .start()
    }

    private fun readOutputInBackground(process: Process, output: StringBuilder): Thread {

        return Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { output.appendLine(it) }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun waitForCommand(process: Process, reader: Thread) {
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            reader.join(1_000)
            error("Bash command timed out after ${timeout.seconds}s")
        }
        reader.join()
    }

    private fun checkCommandResult(process: Process, output: StringBuilder) {
        check(process.exitValue() == 0) {
            "Bash command failed with exit code ${process.exitValue()}:\n$output"
        }
    }

    private fun cmdWithUser(cmd: String): List<String> {
        return listOf("sudo", "-n", "-u", user, "--", "bash", "-c", cmd)
    }
}
