/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.engine

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Boolean.getBoolean
import kotlin.test.fail

private val toolLogsEnabled: Boolean = getBoolean("kotlin.js.test.verbose")

class ExternalTool(val path: String) {
    fun run(vararg arguments: String, workingDirectory: File? = null): String {
        val command = arrayOf(path, *arguments)
        val processBuilder = ProcessBuilder(*command)
            .redirectErrorStream(true)

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

        // Print process output
        val stdout = StringBuilder()
        val bufferedStdout = BufferedReader(InputStreamReader(process.inputStream))

        while (true) {
            val line = bufferedStdout.readLine() ?: break
            stdout.appendLine(line)
            println(line)
        }

        val exitValue = process.waitFor()
        if (exitValue != 0) {
            fail("Command \"$commandString\" terminated with exit code $exitValue")
        }

        return stdout.toString()
    }
}

fun escapeShellArgument(arg: String): String =
    "'${arg.replace("'", "'\\''")}'"
