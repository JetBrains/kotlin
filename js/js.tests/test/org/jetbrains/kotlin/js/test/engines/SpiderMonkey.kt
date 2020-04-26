/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.engines

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.fail

class ExternalTool(val path: String) {
    fun run(vararg arguments: String) {
        val command = arrayOf(path, *arguments)
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        val commandString = command.joinToString(" ") { escapeShellArgument(it) }
        println(commandString)

        // Print process output
        val input = BufferedReader(InputStreamReader(process.inputStream))
        while (true) println(input.readLine() ?: break)

        val exitValue = process.waitFor()
        if (exitValue != 0) {
            fail("Command \"$commandString\" terminated with exit code $exitValue")
        }
    }
}

private fun escapeShellArgument(arg: String): String =
    "'${arg.replace("'", "'\\''")}'"

class SpiderMonkeyEngine(
    jsShellPath: String = System.getProperty("javascript.engine.path.SpiderMonkey")
) {
    private val jsShell = ExternalTool(jsShellPath)

    fun runFile(file: String) {
        jsShell.run("--wasm-gc", file)
    }
}