/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.common.repl.replUnescapeLineBreaks
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.scripting.repl.configuration.ConsoleReplConfiguration
import org.jetbrains.kotlin.scripting.repl.configuration.IdeReplConfiguration
import org.jetbrains.kotlin.scripting.repl.configuration.ReplConfiguration
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ReplFromTerminal(
    disposable: Disposable,
    compilerConfiguration: CompilerConfiguration,
    private val replConfiguration: ReplConfiguration
) {
    private val replInitializer: Future<ReplInterpreter> = Executors.newSingleThreadExecutor().submit(Callable {
        ReplInterpreter(disposable, compilerConfiguration, replConfiguration)
    })

    private val replInterpreter: ReplInterpreter
        get() = replInitializer.get()

    private val writer get() = replConfiguration.writer

    private val messageCollector = compilerConfiguration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    private fun doRun() {
        try {
            with(writer) {
                printlnWelcomeMessage(
                    "Welcome to Kotlin version ${KotlinCompilerVersion.VERSION} " +
                            "(JRE ${System.getProperty("java.runtime.version")})"
                )
                printlnWelcomeMessage("Type :help for help, :quit for quit")
            }

            // Display compiler messages related to configuration and CLI arguments, quit if there are errors
            val hasErrors = messageCollector.hasErrors()
            (messageCollector as? GroupingMessageCollector)?.flush()
            if (hasErrors) return

            var next = WhatNextAfterOneLine.READ_LINE
            while (true) {
                next = one(next)
                if (next == WhatNextAfterOneLine.QUIT) {
                    break
                }
            }
        } catch (e: Exception) {
            replConfiguration.exceptionReporter.report(e)
            throw e
        } finally {
            try {
                replConfiguration.commandReader.flushHistory()
            } catch (e: Exception) {
                replConfiguration.exceptionReporter.report(e)
                throw e
            }

        }
    }

    enum class WhatNextAfterOneLine {
        READ_LINE,
        INCOMPLETE,
        QUIT
    }

    private fun one(next: WhatNextAfterOneLine): WhatNextAfterOneLine {
        var line = replConfiguration.commandReader.readLine(next) ?: return WhatNextAfterOneLine.QUIT

        line = line.replUnescapeLineBreaks()

        if (line.startsWith(":") && (line.length == 1 || line[1] != ':')) {
            val notQuit = oneCommand(line.substring(1))
            return if (notQuit) WhatNextAfterOneLine.READ_LINE else WhatNextAfterOneLine.QUIT
        }

        val lineResult = eval(line)
        return if (lineResult is ReplEvalResult.Incomplete) {
            WhatNextAfterOneLine.INCOMPLETE
        } else {
            WhatNextAfterOneLine.READ_LINE
        }
    }

    private fun eval(line: String): ReplEvalResult {
        val evalResult = replInterpreter.eval(line)
        when (evalResult) {
            is ReplEvalResult.ValueResult, is ReplEvalResult.UnitResult -> {
                writer.notifyCommandSuccess()
                if (evalResult is ReplEvalResult.ValueResult) {
                    writer.outputCommandResult(evalResult.toString())
                }
            }
            is ReplEvalResult.Error.Runtime -> writer.outputRuntimeError(evalResult.message)
            is ReplEvalResult.Error.CompileTime -> writer.outputRuntimeError(evalResult.message)
            is ReplEvalResult.Incomplete -> writer.notifyIncomplete()
        }
        return evalResult
    }

    @Throws(Exception::class)
    private fun oneCommand(command: String): Boolean {
        val split = splitCommand(command)
        if (split.isNotEmpty() && command == "help") {
            writer.printlnHelpMessage(
                "Available commands:\n" +
                        ":help                   show this help\n" +
                        ":quit                   exit the interpreter\n" +
                        ":dump bytecode          dump classes to terminal\n" +
                        ":load <file>            load script from specified file"
            )
            return true
        } else if (split.size >= 2 && split[0] == "dump" && split[1] == "bytecode") {
            replInterpreter.dumpClasses(PrintWriter(System.out))
            return true
        } else if (split.isNotEmpty() && split[0] == "quit") {
            return false
        } else if (split.size >= 2 && split[0] == "load") {
            val fileName = split[1]
            try {
                val scriptText = FileUtil.loadFile(File(fileName))
                eval(scriptText)
            } catch (e: IOException) {
                writer.outputCompileError("Can not load script: ${e.message}")
            }
            return true
        } else {
            writer.printlnHelpMessage("Unknown command\n" + "Type :help for help")
            return true
        }
    }

    companion object {
        private fun splitCommand(command: String): List<String> {
            return Arrays.asList(*command.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray())
        }

        fun run(disposable: Disposable, configuration: CompilerConfiguration) {
            val replIdeMode = System.getProperty("kotlin.repl.ideMode") == "true"
            val replConfiguration = if (replIdeMode) IdeReplConfiguration() else ConsoleReplConfiguration()
            return try {
                ReplFromTerminal(disposable, configuration, replConfiguration).doRun()
            } catch (e: Exception) {
                replConfiguration.exceptionReporter.report(e)
                throw e
            }
        }
    }
}
