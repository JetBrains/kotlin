/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import com.intellij.core.JavaCoreProjectEnvironment
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.repl.ReplClassLoader
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.messageCollector
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmReplCompilerBase
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ReplCompilationState
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.createCompilationContextFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration.ReplConfiguration
import org.jetbrains.kotlin.scripting.definitions.*
import java.io.PrintWriter
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.BasicJvmReplEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.renderError

class ReplInterpreter(
    projectEnvironment: JavaCoreProjectEnvironment,
    private val configuration: CompilerConfiguration,
    private val replConfiguration: ReplConfiguration
) {
    private val hostConfiguration: ScriptingHostConfiguration
    private val compilationConfiguration: ScriptCompilationConfiguration
    private val evaluationConfiguration: ScriptEvaluationConfiguration

    private val replState: JvmReplCompilerState<*>

    companion object {
        private val REPL_LINE_AS_SCRIPT_DEFINITION = object : KotlinScriptDefinition(Any::class) {
            override val name = "Kotlin REPL"
        }

    }

    init {
        hostConfiguration = defaultJvmScriptingHostConfiguration

        val environment = (projectEnvironment as? KotlinCoreEnvironment.ProjectEnvironment)?.let {
            KotlinCoreEnvironment.createForProduction(it, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        }
            ?: KotlinCoreEnvironment.createForProduction(
                projectEnvironment.parentDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

        val context =
            createCompilationContextFromEnvironment(
                ScriptCompilationConfigurationFromDefinition(hostConfiguration, REPL_LINE_AS_SCRIPT_DEFINITION),
                environment,
                ScriptDiagnosticsMessageCollector(environment.messageCollector)
            )

        compilationConfiguration = context.baseScriptCompilationConfiguration
        evaluationConfiguration = ScriptEvaluationConfigurationFromDefinition(hostConfiguration, REPL_LINE_AS_SCRIPT_DEFINITION).with {
            scriptExecutionWrapper<Any> { replConfiguration.executionInterceptor.execute(it) }
        }

        replState = JvmReplCompilerState(
            {
                ReplCompilationState(
                    context,
                    analyzerInit = { context1, resolutionFilter ->
                        ReplCodeAnalyzerBase(context1.environment, implicitsResolutionFilter = resolutionFilter)
                    },
                    implicitsResolutionFilter = ReplImplicitsExtensionsResolutionFilter()
                )
            }
        )
    }

    private val compiler = KJvmReplCompilerBase<ReplCodeAnalyzerBase>(hostConfiguration, replState)
    private val evaluator = BasicJvmReplEvaluator()

    private val lineNumber = AtomicInteger()

    private fun nextSnippet(code: String) =
        code.toScriptSource(
            "Line_${lineNumber.getAndIncrement()}.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}"
        )

    private val previousIncompleteLines = arrayListOf<String>()

    private val classpathRoots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS).mapNotNull { root ->
        when (root) {
            is JvmModulePathRoot -> root.file // TODO: only add required modules
            is JvmClasspathRoot -> root.file
            else -> null
        }
    }

    private val classLoader =
        ReplClassLoader(
            URLClassLoader(
                classpathRoots.map { it.toURI().toURL() }.toTypedArray(),
                ClassLoader.getSystemClassLoader()?.parent
            )
        )

    private val messageCollector = object : MessageCollector {
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

    fun eval(line: String): ReplEvalResult {
        val fullText = (previousIncompleteLines + line).joinToString(separator = "\n")

        try {
            val snippet = nextSnippet(fullText)

            fun SourceCode.Location.toCompilerMessageLocation() =
                CompilerMessageLocation.create(
                    snippet.name,
                    start.line, start.col,
                    snippet.text.lines().getOrNull(start.line - 1)
                )

            fun ResultWithDiagnostics<*>.reportToMessageCollector() {
                for (it in reports) {
                    val diagnosticSeverity = when (it.severity) {
                        ScriptDiagnostic.Severity.ERROR -> CompilerMessageSeverity.ERROR
                        ScriptDiagnostic.Severity.FATAL -> CompilerMessageSeverity.EXCEPTION
                        ScriptDiagnostic.Severity.WARNING -> CompilerMessageSeverity.WARNING
                        else -> continue
                    }
                    messageCollector.report(diagnosticSeverity, it.message, it.location?.toCompilerMessageLocation())
                }
            }

            @Suppress("DEPRECATION_ERROR")
            val evalRes: ReplEvalResult = internalScriptingRunSuspend {
                when (val compileResult = compiler.compile(listOf(snippet), compilationConfiguration)) {
                    is ResultWithDiagnostics.Failure -> {
                        val incompleteReport = compileResult.reports.find { it.code == ScriptDiagnostic.incompleteCode }
                        if (incompleteReport != null)
                            ReplEvalResult.Incomplete(incompleteReport.message)
                        else {
                            compileResult.reportToMessageCollector()
                            ReplEvalResult.Error.CompileTime("")
                        }
                    }
                    is ResultWithDiagnostics.Success -> {
                        compileResult.reportToMessageCollector()
                        val evalResult = evaluator.eval(compileResult.value, evaluationConfiguration)
                        when (evalResult) {
                            is ResultWithDiagnostics.Success -> {
                                when (val evalValue = evalResult.value.get().result) {
                                    is ResultValue.Unit -> ReplEvalResult.UnitResult()
                                    is ResultValue.Value -> ReplEvalResult.ValueResult(evalValue.name, evalValue.value, evalValue.type, evalValue.scriptInstance)
                                    is ResultValue.Error -> ReplEvalResult.Error.Runtime(evalValue.renderError())
                                    else -> ReplEvalResult.Error.Runtime("Error: snippet is not evaluated")
                                }
                            }
                            else -> {
                                evalResult.reportToMessageCollector()
                                ReplEvalResult.Error.Runtime("")
                            }
                        }
                    }
                }
            }

            when {
                evalRes !is ReplEvalResult.Incomplete -> previousIncompleteLines.clear()
                replConfiguration.allowIncompleteLines -> previousIncompleteLines.add(line)
                else -> return ReplEvalResult.Error.CompileTime("incomplete code")
            }
            return evalRes
        } catch (e: Throwable) {
            val writer = PrintWriter(System.err)
            classLoader.dumpClasses(writer)
            writer.flush()
            throw e
        }
    }

    fun dumpClasses(out: PrintWriter) {
        classLoader.dumpClasses(out)
    }
}
