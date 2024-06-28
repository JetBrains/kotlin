/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import java.io.File
import java.io.Serializable
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.util.renderError

abstract class AbstractScriptEvaluationExtension : ScriptEvaluationExtension {

    abstract fun setupScriptConfiguration(configuration: CompilerConfiguration)

    abstract fun createEnvironment(
        projectEnvironment: KotlinCoreEnvironment.ProjectEnvironment,
        configuration: CompilerConfiguration
    ): KotlinCoreEnvironment

    abstract fun createScriptEvaluator(): ScriptEvaluator
    abstract fun createScriptCompiler(environment: KotlinCoreEnvironment): ScriptCompilerProxy

    protected abstract fun ScriptEvaluationConfiguration.Builder.platformEvaluationConfiguration()

    override fun eval(
        arguments: CommonCompilerArguments,
        configuration: CompilerConfiguration,
        projectEnvironment: KotlinCoreEnvironment.ProjectEnvironment
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(projectEnvironment.project)
        if (scriptDefinitionProvider == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Unable to process the script, scripting plugin is not configured")
            return ExitCode.COMPILATION_ERROR
        }

        setupScriptConfiguration(configuration)

        val defaultScriptExtension =
            (arguments as? K2JVMCompilerArguments)?.defaultScriptExtension?.let { if (it.startsWith('.')) it else ".$it" }

        val script = when {
            arguments is K2JVMCompilerArguments && arguments.expression != null -> {
                StringScriptSource(arguments.expression!!, "script${defaultScriptExtension ?: ".kts"}")
            }
            arguments.script -> {
                val scriptFile = File(arguments.freeArgs.first()).normalize()

                fun invalidScript(error: String): ExitCode {
                    val extensionHint =
                        if (configuration.get(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS)
                                ?.let { it.size == 1 && it.first().isDefault } == true
                        ) " (.kts)"
                        else ""
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "$error; Specify path to the script file$extensionHint as the first argument"
                    )
                    return ExitCode.COMPILATION_ERROR
                }

                if (!scriptFile.exists()) return invalidScript("Script file not found: $scriptFile")

                if (scriptFile.isDirectory) return invalidScript("Script argument points to a directory: $scriptFile")

                var script = scriptFile.toScriptSource().takeIf {
                    scriptDefinitionProvider.isScript(it)
                }
                if (script == null && defaultScriptExtension != null) {
                    script = ExplicitlyNamedFileScriptSource(
                        scriptFile.nameWithoutExtension + defaultScriptExtension, scriptFile
                    ).takeIf {
                        scriptDefinitionProvider.isScript(it)
                    }
                }
                script ?: return invalidScript("Unrecognized script type: ${scriptFile.name}")
            }
            else -> {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Illegal set of arguments: either -script or -expression arguments expected at this point"
                )
                return ExitCode.COMPILATION_ERROR
            }
        }

        val environment = createEnvironment(projectEnvironment, configuration)

        if (messageCollector.hasErrors()) return ExitCode.COMPILATION_ERROR

        val definition = scriptDefinitionProvider.findDefinition(script) ?: scriptDefinitionProvider.getDefaultDefinition()

        val scriptCompilationConfiguration = definition.compilationConfiguration

        val scriptArgs =
            if (arguments.script) arguments.freeArgs.subList(1, arguments.freeArgs.size)
            else arguments.freeArgs

        val evaluationConfiguration = definition.evaluationConfiguration.with {
            constructorArgs(scriptArgs.toTypedArray())
            platformEvaluationConfiguration()

        }
        return doEval(script, scriptCompilationConfiguration, evaluationConfiguration, environment, messageCollector)
    }

    private fun doEval(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration,
        environment: KotlinCoreEnvironment,
        messageCollector: MessageCollector
    ): ExitCode {
        val scriptCompiler = createScriptCompiler(environment)

        @Suppress("DEPRECATION_ERROR")
        return internalScriptingRunSuspend {
            val compiledScript = scriptCompiler.compile(script, scriptCompilationConfiguration).valueOr {
                val lines = if (it.reports.isEmpty()) null else script.text.lines()
                for (report in it.reports) {
                    val location = report.location
                    val sourcePath = report.sourcePath
                    messageCollector.report(
                        report.severity.toCompilerMessageSeverity(),
                        report.render(withSeverity = false, withLocation = location == null || sourcePath == null),
                        if (location != null && sourcePath != null) {
                            CompilerMessageLocation.create(
                                sourcePath,
                                location.start.line, location.start.col,
                                lines?.getOrNull(location.start.line - 1)
                            )
                        } else null
                    )
                }
                return@internalScriptingRunSuspend ExitCode.COMPILATION_ERROR
            }

            val evalResult = createScriptEvaluator().invoke(compiledScript, evaluationConfiguration).valueOr {
                for (report in it.reports) {
                    messageCollector.report(report.severity.toCompilerMessageSeverity(), report.render(withSeverity = false))
                }
                return@internalScriptingRunSuspend ExitCode.INTERNAL_ERROR
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

fun ScriptDiagnostic.Severity.toCompilerMessageSeverity(): CompilerMessageSeverity =
    when (this) {
        ScriptDiagnostic.Severity.FATAL -> CompilerMessageSeverity.EXCEPTION
        ScriptDiagnostic.Severity.ERROR -> CompilerMessageSeverity.ERROR
        ScriptDiagnostic.Severity.WARNING -> CompilerMessageSeverity.WARNING
        ScriptDiagnostic.Severity.INFO -> CompilerMessageSeverity.INFO
        ScriptDiagnostic.Severity.DEBUG -> CompilerMessageSeverity.LOGGING
    }

open class ExplicitlyNamedFileScriptSource(
    override val name: String, file: File, preloadedText: String? = null
) : FileScriptSource(file, preloadedText), Serializable {

    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }
}
