/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import com.intellij.core.JavaCoreProjectEnvironment
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import java.io.File
import java.io.PrintStream
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator

class JvmCliScriptEvaluationExtension : ScriptEvaluationExtension {
    override fun isAccepted(arguments: CommonCompilerArguments): Boolean =
        arguments is K2JVMCompilerArguments && arguments.script

    override fun eval(
        arguments: CommonCompilerArguments,
        configuration: CompilerConfiguration,
        projectEnvironment: JavaCoreProjectEnvironment
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(projectEnvironment.project)
        if (scriptDefinitionProvider == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Unable to process the script, scripting plugin is not configured")
            return COMPILATION_ERROR
        }
        val sourcePath = arguments.freeArgs.first()

        configuration.addKotlinSourceRoot(sourcePath)
        configuration.put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
        val environment =
            KotlinCoreEnvironment.createForProduction(projectEnvironment, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        if (messageCollector.hasErrors()) return COMPILATION_ERROR

        val scriptFile = File(sourcePath)
        if (scriptFile.isDirectory || !scriptDefinitionProvider.isScript(scriptFile)) {
            val extensionHint =
                if (configuration.get(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS)?.let { it.size == 1 && it.first().isDefault } == true) " (.kts)"
                else ""
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify path to the script file$extensionHint as the first argument")
            return COMPILATION_ERROR
        }

        val script = scriptFile.toScriptSource()

        val definition = scriptDefinitionProvider.findDefinition(scriptFile) ?: scriptDefinitionProvider.getDefaultDefinition()

        val scriptArgs = arguments.freeArgs.subList(1, arguments.freeArgs.size)

        val evaluationConfiguration = definition.evaluationConfiguration.with {
            constructorArgs(scriptArgs.toTypedArray())
        }
        val scriptCompilationConfiguration = definition.compilationConfiguration

        val scriptCompiler = ScriptJvmCompilerFromEnvironment(environment)

        return runBlocking {
            val compiledScript =
                scriptCompiler.compile(script, scriptCompilationConfiguration)
                    .valueOr {
                        for (report in it.reports) {
                            messageCollector.report(report.severity.toCompilerMessageSeverity(), report.render(withSeverity = false))
                        }
                        return@runBlocking COMPILATION_ERROR
                    }
            val evalResult = BasicJvmScriptEvaluator().invoke(compiledScript, evaluationConfiguration)
                .valueOr {
                    for (report in it.reports) {
                        messageCollector.report(report.severity.toCompilerMessageSeverity(), report.render(withSeverity = false))
                    }
                    return@runBlocking ExitCode.INTERNAL_ERROR
                }
            when (evalResult.returnValue) {
                is ResultValue.Value -> {
                    println((evalResult.returnValue as ResultValue.Value).value)
                    ExitCode.OK
                }
                is ResultValue.Error -> {
                    val errorValue = evalResult.returnValue as ResultValue.Error
                    errorValue.renderError(System.err)
                    ExitCode.SCRIPT_EXECUTION_ERROR
                }
                else -> ExitCode.OK
            }
        }
    }
}

private fun ScriptDiagnostic.Severity.toCompilerMessageSeverity(): CompilerMessageSeverity =
    when (this) {
        ScriptDiagnostic.Severity.FATAL -> CompilerMessageSeverity.EXCEPTION
        ScriptDiagnostic.Severity.ERROR -> CompilerMessageSeverity.ERROR
        ScriptDiagnostic.Severity.WARNING -> CompilerMessageSeverity.WARNING
        ScriptDiagnostic.Severity.INFO -> CompilerMessageSeverity.INFO
        ScriptDiagnostic.Severity.DEBUG -> CompilerMessageSeverity.LOGGING
    }

private fun ResultValue.Error.renderError(stream: PrintStream) {
    val fullTrace = error.stackTrace
    if (wrappingException == null || fullTrace.size < wrappingException!!.stackTrace.size) {
        error.printStackTrace(stream)
    } else {
        // subtracting wrapping message stacktrace from error stacktrace to show only user-specific part of it
        // TODO: consider more reliable logic, e.g. comparing traces, fallback to full error printing in case of mismatch
        // TODO: write tests
        stream.println(error)
        val scriptTraceSize = fullTrace.size - wrappingException!!.stackTrace.size
        for (i in 0 until scriptTraceSize) {
            stream.println("\tat " + fullTrace[i])
        }
    }
}
