/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.engine

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Boolean.getBoolean

private val toolLogsEnabled: Boolean = getBoolean("kotlin.js.test.verbose")

class ExternalTool(val path: String) {
    fun run(vararg arguments: String, workingDirectory: File? = null): String {
        val command = arrayOf(path, *arguments)
        val processBuilder = ProcessBuilder(*command)

        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory)
        }

        val process = processBuilder.start()

        val commandString = command.joinToString(" ") { escapeShellArgument(it) }
        if (toolLogsEnabled) {
            println(
                if (workingDirectory != null) {
                    "(cd '$workingDirectory' && $commandString)"
                } else {
                    commandString
                }
            )
        }

        val stdout = dumpStream(process.inputStream)
        val stderr = dumpStream(process.errorStream)

        val exitValue = process.waitFor()
        if (exitValue != 0) {
            throw ScriptExecutionException(stdout, stderr)
        }

        return stdout
    }

    private fun dumpStream(stream: InputStream): String = buildString {
        val reader = BufferedReader(InputStreamReader(stream))
        while (true) {
            val line = reader.readLine() ?: break
            appendLine(line)
        }
    }
}

fun escapeShellArgument(arg: String): String =
    "'${arg.replace("'", "'\\''")}'"
