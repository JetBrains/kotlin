package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.BaseGradleIT
import java.io.File

class ProcessRunResult(
        private val cmd: List<String>,
        private val workingDir: File,
        val exitCode: Int,
        val output: String
) {
    val isSuccessful: Boolean
        get() = exitCode == 0

    override fun toString(): String = """
Executing process was ${if (isSuccessful) "successful" else "unsuccessful"}
    Command: ${cmd.joinToString()}
    Working directory: ${workingDir.absolutePath}
    Exit code: $exitCode
"""
}

fun runProcess(
        cmd: List<String>,
        workingDir: File,
        environmentVariables: Map<String, String> = mapOf(),
        options: BaseGradleIT.BuildOptions? = null
): ProcessRunResult {
    val builder = ProcessBuilder(cmd)
    builder.environment().putAll(environmentVariables)
    builder.directory(workingDir)
    // redirectErrorStream merges stdout and stderr, so it can be get from process.inputStream
    builder.redirectErrorStream(true)

    val process = builder.start()
    // important to read inputStream, otherwise the process may hang on some systems
    val sb = StringBuilder()
    process.inputStream!!.bufferedReader().forEachLine {
        if (options?.forceOutputToStdout ?: false) {
            System.out.println(it)
        }
        sb.appendln(it)
    }
    val exitCode = process.waitFor()

    return ProcessRunResult(cmd, workingDir, exitCode, sb.toString())
}

fun createGradleCommand(tailParameters: List<String>): List<String> {
    return if (isWindows())
        listOf("cmd", "/C", "gradlew.bat") + tailParameters
    else
        listOf("/bin/bash", "./gradlew") + tailParameters
}

private fun isWindows(): Boolean = System.getProperty("os.name")!!.contains("Windows")