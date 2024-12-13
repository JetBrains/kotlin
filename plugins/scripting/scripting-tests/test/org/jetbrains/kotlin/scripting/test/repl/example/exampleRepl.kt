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
import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.cli.common.repl.LineId
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.common.repl.replUnescapeLineBreaks
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.createIsolatedCompilationContext
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.currentLineId
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplFromTerminal.WhatNextAfterOneLine
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration.ConsoleReplConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration.ReplConfiguration
import org.jetbrains.kotlin.scripting.test.repl.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.test.repl.TestReplCompilerPluginRegistrar
import org.jetbrains.kotlin.scripting.test.repl.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.test.repl.replStateObjectFqName
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.api.with
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.isIncomplete

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

object ReplState: HashMap<String, Any?>()

@OptIn(ExperimentalCompilerApi::class)
private class ExampleRepl(val replConfiguration: ReplConfiguration, rootDisposable: Disposable) {

    private val writer = replConfiguration.writer
    private val messageCollector = ScriptDiagnosticsMessageCollector(ReplMessageCollector(replConfiguration))
    private val scriptCompilationConfiguration = ScriptCompilationConfiguration {
        jvm {
            dependenciesFromClassloader(classLoader = ExampleRepl::class.java.classLoader, wholeClasspath = true)
        }
    }
    val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
        firReplHistoryProvider(FirReplHistoryProviderImpl())
        replStateObjectFqName(ReplState::class.qualifiedName!!)
    }
    private val compilerContext = createIsolatedCompilationContext(
        scriptCompilationConfiguration,
        hostConfiguration,
        messageCollector,
        rootDisposable
    ) {
        add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, TestReplCompilerPluginRegistrar(hostConfiguration))
    }

    private val compiler = ScriptJvmCompilerFromEnvironment(compilerContext.environment)
    private var lineCounter = 0
    private val previousLines = mutableListOf<LineId>()
    private var lastSnippetClass: KClass<*>? = null
    private val replState: MutableMap<String, Any?> = mutableMapOf()

    suspend fun repl() {
        try {
            with(writer) {
                printlnWelcomeMessage("Welcome to Kotlin version ${KotlinCompilerVersion.VERSION} (JRE ${System.getProperty("java.runtime.version")})")
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

    private fun compile(line: String, lineNo: Int, previousLines: List<ILineId>): ReplCompileResult {
        val lineId = LineId(lineNo, 0, line.hashCode())
        val snippet = line.toScriptSource("snippet_$lineNo.repl.kts")
        val res =
            compiler.compile(
                snippet,
                scriptCompilationConfiguration.with {
                    repl {
                        currentLineId(lineId)
                    }
                }
            )
        return when (res) {
            is ResultWithDiagnostics.Success -> {
                ReplCompileResult.CompiledClasses(
                    lineId,
                    previousLines,
                    snippet.name!!,
                    emptyList(),
                    res.value.resultField != null,
                    emptyList(),
                    res.value.resultField?.second?.typeName,
                    res.value
                )
            }
            else -> {
                val message = res.reports.joinToString("\n")
                if (res.isIncomplete()) {
                    ReplCompileResult.Incomplete(message)
                } else {
                    ReplCompileResult.Error(message)
                }
            }
        }
    }

    private suspend fun compileAndEval(line: String, lineNo: Int): ReplEvalResult {
        val compileResult = compile(line, lineNo, previousLines)
        return when (compileResult) {
            is ReplCompileResult.CompiledClasses -> {

                val compiledScript = (compileResult.data as? KJvmCompiledScript)
                    ?: return ReplEvalResult.Error.CompileTime("Unable to access compiled script")

                val currentConfiguration = ScriptEvaluationConfiguration {
                    jvm {
                        baseClassLoader(lastSnippetClass?.java?.classLoader ?: this@ExampleRepl::class.java.classLoader)
                    }
                }
                val snippetClass = compiledScript.getClass(currentConfiguration)
                if (snippetClass is ResultWithDiagnostics.Success) {
                    try {
                        val ctor = snippetClass.value.java.constructors.single()
                        val snippet = ctor.newInstance(replState)
                        val eval = snippetClass.value.java.methods.find { it.name.contains("eval") }!!
                        val res = eval.invoke(snippet)

                        previousLines.add(compileResult.lineId)
                        lastSnippetClass = snippetClass.value

                        if (res != null)
                            ReplEvalResult.ValueResult(compiledScript.resultField?.first ?: "res", res, compiledScript.resultField?.second?.typeName ?: "kotlin.Any?", snippetClass)
                        else
                            ReplEvalResult.UnitResult()
                    } catch (t: Throwable) {
                        ReplEvalResult.Error.Runtime("Error running snippet", t)
                    }
                } else {
                    ReplEvalResult.Error.Runtime(snippetClass.reports.joinToString("\n"))
                }
            }
            is ReplCompileResult.Error -> ReplEvalResult.Error.CompileTime(compileResult.message)
            is ReplCompileResult.Incomplete -> ReplEvalResult.Incomplete(compileResult.message)
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
