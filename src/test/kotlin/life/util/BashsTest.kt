package life.util

import life.util.data.Cmd
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class BashsTest {
    private val user = System.getProperty("user.name")
    private val workdir = System.getProperty("user.dir")

    @Test
    fun `uses sudo in non interactive mode`() {
        assertEquals(
            listOf("sudo", "-n", "-u", "app", "--", "bash", "-c", "whoami"),
            listOf("sudo", "-n", "-u", "app", "--", "bash", "-c", "whoami"),
        )
    }

    @Test
    fun `returns stdout when sudo is available`() {
        if (!canSudoWithoutPassword()) {
            return
        }

        assertEquals("hello\n", Bashs.exec(user, workdir, "printf 'hello\\n'"))
    }

    @Test
    fun `returns stdout from cmd input when sudo is available`() {
        if (!canRunRestrictedCmd()) {
            return
        }

        assertEquals(
            "hello\n",
            Bashs.exec(Cmd(command = "printf 'hello\\n'")),
        )
    }

    @Test
    fun `returns stderr with stdout like terminal output when sudo is available`() {
        if (!canSudoWithoutPassword()) {
            return
        }

        assertEquals("out\nerr\n", Bashs.exec(user, workdir, "printf 'out\\n'; printf 'err\\n' >&2"))
    }

    @Test
    fun `returns output without wrapping non zero exit code when sudo is available`() {
        if (!canSudoWithoutPassword()) {
            return
        }

        assertEquals("failed\n", Bashs.exec(user, workdir, "printf 'failed\\n'; exit 7"))
    }

    @Test
    fun `executes command in workdir when sudo is available`() {
        if (!canSudoWithoutPassword()) {
            return
        }

        val dir = Files.createTempDirectory("bashs-test").toRealPath().toString()

        assertEquals("$dir\n", Bashs.exec(user, dir, "pwd -P"))
    }

    private fun canSudoWithoutPassword(): Boolean {
        return runCatching {
            val process = ProcessBuilder(listOf("sudo", "-n", "-u", user, "--", "bash", "-c", "true"))
                .redirectErrorStream(true)
                .start()

            process.inputStream.readAllBytes()
            process.waitFor() == 0
        }.getOrDefault(false)
    }

    private fun canRunRestrictedCmd(): Boolean {
        return runCatching {
            Bashs.exec(Cmd(command = "true"))
            true
        }.getOrDefault(false)
    }
}
