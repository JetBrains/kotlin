/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.exec

import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect
import org.jetbrains.kotlin.konan.KonanExternalToolFailure


open class Command(initialCommand: List<String>) {

    constructor(tool: String) : this(listOf(tool)) 
    constructor(vararg command: String) : this(command.toList<String>()) 
    protected val command = initialCommand.toMutableList()

    val args: List<String> 
        get() = command.drop(1)

    operator fun String.unaryPlus(): Command {
        command += this
        return this@Command
    }

    operator fun List<String>.unaryPlus(): Command {
        command.addAll(this)
        return this@Command
    }

    var logger: ((() -> String)->Unit)? = null

    fun logWith(newLogger: ((() -> String)->Unit)): Command {
        logger = newLogger
        return this
    }

    open fun runProcess(): Int {
        val builder = ProcessBuilder(command)

        builder.redirectOutput(Redirect.INHERIT)
        builder.redirectInput(Redirect.INHERIT)
        builder.redirectError(Redirect.INHERIT)

        val process = builder.start()
        val exitCode = process.waitFor()
        return exitCode
    }

    open fun execute() {
        log()

        val code = runProcess()
        handleExitCode(code)
    }

    /**
     * If withErrors is true then output from error stream will be added
     */
    fun getOutputLines(withErrors: Boolean = false): List<String> =
            getResult(withErrors, handleError = true).outputLines

    fun getResult(withErrors: Boolean, handleError: Boolean = false): Result {
        log()

        val outputFile = createTempFile()
        outputFile.deleteOnExit()

        try {
            val builder = ProcessBuilder(command)

            builder.redirectInput(Redirect.INHERIT)
            builder.redirectError(Redirect.INHERIT)
            builder.redirectOutput(Redirect.to(outputFile))
                    .redirectErrorStream(withErrors)
            // Note: getting process output could be done without redirecting to temporary file,
            // however this would require managing a thread to read `process.inputStream` because
            // it may have limited capacity.

            val process = builder.start()
            val code = process.waitFor()
            if (handleError) handleExitCode(code, outputFile.readLines())

            return Result(code, outputFile.readLines())
        } finally {
            outputFile.delete()
        }
    }

    class Result(val exitCode: Int, val outputLines: List<String>)

    private fun handleExitCode(code: Int, output: List<String> = emptyList()) {
        if (code != 0) throw KonanExternalToolFailure("""
            The ${command[0]} command returned non-zero exit code: $code.
            output: ${output.joinToString("\n")}
            """.trimIndent(), command[0])
    }

    private fun log() {
        if (logger != null) logger!! { command.joinToString(" ") }
    }
}
