package org.jetbrains.kotlin.gradle.util

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
    Output: $output
"""
}

fun runProcess(cmd: List<String>, workingDir: File): ProcessRunResult {
    val builder = ProcessBuilder(cmd)
    builder.directory(workingDir)
    // redirectErrorStream merges stdout and stderr, so it can be get from process.inputStream
    builder.redirectErrorStream(true)

    val process = builder.start()
    // important to read inputStream, otherwise the process may hang on some systems
    val output = process.inputStream!!.bufferedReader().readText()
    System.out.println(output)
    val exitCode = process.waitFor()

    return ProcessRunResult(cmd, workingDir, exitCode, output)
}

fun createGradleCommand(tailParameters: List<String>): List<String> {
    return if (isWindows())
        listOf("cmd", "/C", "gradlew.bat") + tailParameters
    else
        listOf("/bin/bash", "./gradlew") + tailParameters
}

private fun isWindows(): Boolean = System.getProperty("os.name")!!.contains("Windows")