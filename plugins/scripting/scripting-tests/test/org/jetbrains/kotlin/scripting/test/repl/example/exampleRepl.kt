/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.repl.example

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.common.repl.replUnescapeLineBreaks
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplEvaluator
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.currentLineId
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplFromTerminal.WhatNextAfterOneLine
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration.ConsoleReplConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration.ReplConfiguration
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.isIncomplete
import kotlin.script.experimental.jvm.withUpdatedClasspath
import kotlin.script.experimental.util.LinkedSnippet

/**
 * Annotation for adding dependencies.
 * [value] should be an absolute path to a jar file.
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOn(val value: String = "")

fun onAnnotationsHandler(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    // Simplistic way of finding the value of @DependsOn annotations (to keep line-count down in this file)
    val files = context.script.text.lines().filter {
        it.startsWith("@file:DependsOn")
    }.map {
        it.removePrefix("@file:DependsOn(\"").removeSuffix("\")")
    }
    return context.compilationConfiguration.withUpdatedClasspath(
        files.map { File(it) }
    ).asSuccess()
}

/**
 * Test K2 REPL implementation. Very Experimental! Do not use! May break at any moment!
 */
fun main() {
    val disposable = Disposer.newDisposable("Disposable example REPL")
    try {
        val replWrapper = ExampleRepl(ConsoleReplConfiguration(), disposable)
        @Suppress("DEPRECATION_ERROR")
        internalScriptingRunSuspend {
            replWrapper.repl()
        }
    } finally {
        Disposer.dispose(disposable)
    }
}

@OptIn(ExperimentalCompilerApi::class)
private class ExampleRepl(val replConfiguration: ReplConfiguration, rootDisposable: Disposable) {

    private val writer = replConfiguration.writer
    private val messageCollector = ScriptDiagnosticsMessageCollector(ReplMessageCollector(replConfiguration))
    private var lineCounter = 0

    private val initialScriptCompilationConfiguration = ScriptCompilationConfiguration {
        jvm {
            updateClasspath(
                listOf(
                    StandardLibrariesPathProviderForKotlinProject.runtimeJarForTests(),
                    // Make sure that the DependsOn annotation is on the classpath
                    File("plugins/scripting/scripting-tests/build/classes/kotlin/test")
                )
            )
        }
        defaultImports(
            listOf("org.jetbrains.kotlin.scripting.test.repl.example.DependsOn")
        )
        refineConfiguration {
            onAnnotations(DependsOn::class, handler = ::onAnnotationsHandler)
        }
    }

    private var scriptCompilationConfiguration = initialScriptCompilationConfiguration

    private val replCompiler =
        K2ReplCompiler(
            K2ReplCompiler.createCompilationState(
                messageCollector,
                rootDisposable,
                scriptCompilationConfiguration
            )
        )

    var evaluationConfiguration =
        ScriptEvaluationConfiguration {
            jvm {
                baseClassLoader( ExampleRepl::class.java.classLoader)
            }
        }

    val replEvaluator = K2ReplEvaluator()

    suspend fun repl() {
        try {
            with(writer) {
                printlnWelcomeMessage("Welcome to Kotlin Test Repl")
                printlnWelcomeMessage("version ${KotlinCompilerVersion.VERSION} (JRE ${System.getProperty("java.runtime.version")})")
                printlnWelcomeMessage("Type :help for help, :quit for quit")
            }
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

    private suspend fun one(next: WhatNextAfterOneLine): WhatNextAfterOneLine {
        var line = replConfiguration.commandReader.readLine(next) ?: return WhatNextAfterOneLine.QUIT

        line = line.replUnescapeLineBreaks()

        if (line.startsWith(":") && (line.length == 1 || line[1] != ':')) {
            val notQuit = oneCommand(line.substring(1))
            return if (notQuit) WhatNextAfterOneLine.READ_LINE else WhatNextAfterOneLine.QUIT
        }

        val lineResult = compileAndEval(line, ++lineCounter)
        when (lineResult) {
            is ReplEvalResult.Error.Runtime -> {
                writer.sendInternalErrorReport(lineResult.message)
            }
            is ReplEvalResult.Error.CompileTime -> {
                writer.outputCompileError(lineResult.message)
            }
            is ReplEvalResult.ValueResult -> {
                writer.outputCommandResult("-> ${lineResult.value} : ${lineResult.type ?: "kotlin.Any?"}")
            }
            else -> {}
        }
        return if (lineResult is ReplEvalResult.Incomplete) {
            WhatNextAfterOneLine.INCOMPLETE
        } else {
            WhatNextAfterOneLine.READ_LINE
        }
    }

    private fun oneCommand(command: String): Boolean =
        when (command) {
            "help", "h", "?" -> {
                writer.printlnHelpMessage("Available commands:")
                writer.printlnHelpMessage(":help(:h,:?)        - display this help")
                writer.printlnHelpMessage(":quit(:q)           - exit the interpreter")
                true
            }
            "quit", "q" -> {
                false
            }
            else -> {
                writer.outputCompileError("Unknown command :$command")
                true
            }
        }

    private suspend fun compile(line: String, lineNo: Int): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> {
        val lineId = LineId(lineNo, 0, line.hashCode())
        val snippet = line.toScriptSource("snippet_$lineNo.repl.kts")

        return replCompiler.compile(
            snippet,
            scriptCompilationConfiguration.with {
                repl {
                    currentLineId(lineId)
                }
            }
        ).also {
            if (it is ResultWithDiagnostics.Success) {
                scriptCompilationConfiguration = it.value.get().compilationConfiguration
            }
        }
    }

    private suspend fun compileAndEval(line: String, lineNo: Int): ReplEvalResult {
        val compileAndEvalResult = compile(line, lineNo).onSuccess {
            replEvaluator.eval(it, evaluationConfiguration)
        }
        return when (compileAndEvalResult) {
            is ResultWithDiagnostics.Success -> {
                when (val evaluationResult = compileAndEvalResult.value.get().result) {
                    is ResultValue.Value ->
                        ReplEvalResult.ValueResult(
                            evaluationResult.name, evaluationResult.value, evaluationResult.type, evaluationResult.scriptClass
                        )
                    is ResultValue.Unit -> ReplEvalResult.UnitResult()
                    is ResultValue.Error -> ReplEvalResult.Error.Runtime(compileAndEvalResult.reports.joinToString("\n"))
                    ResultValue.NotEvaluated -> ReplEvalResult.Error.Runtime("Not evaluated")
                }
            }
            is ResultWithDiagnostics.Failure -> {
                when {
                    compileAndEvalResult.isIncomplete() -> ReplEvalResult.Incomplete(compileAndEvalResult.reports.joinToString("\n"))
                    else -> ReplEvalResult.Error.CompileTime(compileAndEvalResult.reports.joinToString("\n"))
                }
            }
        }
    }
}

private class ReplMessageCollector(val replConfiguration: ReplConfiguration) : MessageCollector {
    private var hasErrors = false
    private val messageRenderer = MessageRenderer.WITHOUT_PATHS

    override fun clear() {
        hasErrors = false
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val msg = messageRenderer.render(severity, message, location).trimEnd()
        with(replConfiguration.writer) {
            when (severity) {
                CompilerMessageSeverity.EXCEPTION -> sendInternalErrorReport(msg)
                CompilerMessageSeverity.ERROR -> outputCompileError(msg)
                CompilerMessageSeverity.STRONG_WARNING -> {
                } // TODO consider reporting this and two below
                CompilerMessageSeverity.WARNING -> {
                }
                CompilerMessageSeverity.INFO -> {
                }
                else -> {
                }
            }
        }
    }

    override fun hasErrors(): Boolean = hasErrors
}
