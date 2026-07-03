/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.defaultExtensionForScripts
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.freeArgsForScript
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.scriptMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors.CheckDiagnosticCollector
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportException
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.expressionToEvaluate
import org.jetbrains.kotlin.config.scriptingHostConfiguration
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplStatelessCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifact
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactCodec
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import java.io.File
import java.io.Serializable
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.renderError

abstract class AbstractScriptEvaluationExtension : ScriptEvaluationExtension {

    abstract fun setupScriptConfiguration(configuration: CompilerConfiguration)

    @K1Deprecation
    abstract fun createEnvironment(
        projectEnvironment: KotlinCoreEnvironment.ProjectEnvironment,
        configuration: CompilerConfiguration
    ): KotlinCoreEnvironment

    abstract fun createScriptEvaluator(): ScriptEvaluator

    @Deprecated("Use and Implement createScriptCompiler(KotlinCoreEnvironment, ScriptCompilationConfiguration) method")
    abstract fun createScriptCompiler(environment: KotlinCoreEnvironment): ScriptCompilerProxy

    abstract fun createScriptCompiler(
        environment: KotlinCoreEnvironment, scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ScriptCompilerProxy

    protected abstract fun ScriptEvaluationConfiguration.Builder.platformEvaluationConfiguration()

    override fun eval(
        arguments: CommonCompilerArguments,
        configuration: CompilerConfiguration,
        projectEnvironment: KotlinCoreEnvironment.ProjectEnvironment
    ): ExitCode {
        val jvmArgs = arguments as? K2JVMCompilerArguments
        return eval(
            configuration,
            projectEnvironment,
            jvmArgs?.defaultScriptExtension,
            jvmArgs?.expression,
            arguments.script,
            arguments.freeArgs
        )
    }

    override fun eval(
        configuration: CompilerConfiguration,
        projectEnvironment: KotlinCoreEnvironment.ProjectEnvironment,
    ): ExitCode {
        return eval(
            configuration,
            projectEnvironment,
            configuration.defaultExtensionForScripts,
            configuration.expressionToEvaluate,
            configuration.scriptMode,
            configuration.freeArgsForScript
        )
    }

    private fun eval(
        configuration: CompilerConfiguration,
        projectEnvironment: KotlinCoreEnvironment.ProjectEnvironment,
        defaultScriptExtensionFromArguments: String?,
        expressionToEvaluate: String?,
        scriptMode: Boolean,
        freeArgs: List<String>,
    ): ExitCode {
        val scriptDefinitionProvider = configuration.getCompilerExtensions(ScriptDefinitionProvider).firstOrNull()
        if (scriptDefinitionProvider == null) {
            configuration.report(CliDiagnostics.SCRIPTING_ERROR, "Unable to process the script, scripting plugin is not configured")
            return ExitCode.COMPILATION_ERROR
        }

        setupScriptConfiguration(configuration)

        val defaultScriptExtension = defaultScriptExtensionFromArguments?.let { if (it.startsWith('.')) it else ".$it" }

        val script = when {
            expressionToEvaluate != null -> {
                StringScriptSource(expressionToEvaluate, "script${defaultScriptExtension ?: ".kts"}")
            }
            scriptMode -> {
                val scriptFile = File(freeArgs.first()).normalize()

                fun invalidScript(error: String): ExitCode {
                    val extensionHint =
                        if (configuration.get(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS)
                                ?.let { it.size == 1 && it.first().isDefault } == true
                        ) " (.kts)"
                        else ""
                    configuration.report(
                        CliDiagnostics.SCRIPTING_ERROR,
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
                        scriptFile.name.removeSuffix(".kts") + defaultScriptExtension, scriptFile
                    ).takeIf {
                        scriptDefinitionProvider.isScript(it)
                    }
                }
                script ?: return invalidScript("Unrecognized script type: ${scriptFile.name}")
            }
            else -> {
                configuration.report(
                    CliDiagnostics.SCRIPTING_ERROR,
                    "Illegal set of arguments: either -script or -expression arguments expected at this point"
                )
                return ExitCode.COMPILATION_ERROR
            }
        }

        // Stateless K2 REPL snippet compilation (migration step 3, Q5d): when the snippet-mode
        // plugin option is set, the source is compiled as a REPL snippet against the prior-snippet
        // artifacts on the *regular* compile entry — no evaluation, no daemon REPL transport — and
        // the produced artifact is written to the configured output path.
        if (configuration.getBoolean(ScriptingConfigurationKeys.REPL_SNIPPET_COMPILATION_MODE)) {
            return compileReplSnippet(script, configuration)
        }

        @OptIn(K1Deprecation::class)
        val environment = createEnvironment(projectEnvironment, configuration)

        if (CheckDiagnosticCollector.checkHasErrorsAndReportToMessageCollector(configuration)) return ExitCode.COMPILATION_ERROR

        val definition = scriptDefinitionProvider.findDefinition(script) ?: scriptDefinitionProvider.getDefaultDefinition()

        val scriptCompilationConfiguration = definition.compilationConfiguration

        val scriptArgs =
            if (scriptMode) freeArgs.subList(1, freeArgs.size)
            else freeArgs

        val evaluationConfiguration = definition.evaluationConfiguration.with {
            constructorArgs(scriptArgs.toTypedArray())
            platformEvaluationConfiguration()

        }
        return doEval(script, scriptCompilationConfiguration, evaluationConfiguration, environment, configuration)
    }

    private fun doEval(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration,
        environment: KotlinCoreEnvironment,
        configuration: CompilerConfiguration,
    ): ExitCode {
        val scriptCompiler = createScriptCompiler(environment, scriptCompilationConfiguration)

        @Suppress("DEPRECATION_ERROR")
        return internalScriptingRunSuspend {
            val compiledScript = scriptCompiler.compile(script, scriptCompilationConfiguration).valueOr {
                val lines = if (it.reports.isEmpty()) null else script.text.lines()
                for (report in it.reports) {
                    val location = report.location
                    val sourcePath = report.sourcePath
                    configuration.report(
                        report.severity,
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
                    configuration.report(report.severity, report.render(withSeverity = false), null)
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

internal fun CompilerConfiguration.report(
    severity: ScriptDiagnostic.Severity,
    message: String,
    compilerMessageLocation: CompilerMessageLocation?,
) {
    when (severity) {
        ScriptDiagnostic.Severity.DEBUG -> reportLog(message, compilerMessageLocation)
        ScriptDiagnostic.Severity.INFO -> reportInfo(message, compilerMessageLocation)
        ScriptDiagnostic.Severity.WARNING -> report(CliDiagnostics.SCRIPTING_WARNING, message, compilerMessageLocation)
        ScriptDiagnostic.Severity.ERROR -> report(CliDiagnostics.SCRIPTING_ERROR, message, compilerMessageLocation)
        ScriptDiagnostic.Severity.FATAL -> reportException(message, compilerMessageLocation)
    }
}

/**
 * Stateless K2 REPL snippet compilation driven from the *regular* CLI/daemon compile entry
 * (migration step 3, Q5d). Triggered by [ScriptingConfigurationKeys.REPL_SNIPPET_COMPILATION_MODE],
 * it decodes the prior-snippet artifacts named by
 * [ScriptingConfigurationKeys.REPL_SNIPPET_PRIOR_ARTIFACTS] (in order), drives
 * [K2ReplStatelessCompiler], and writes the produced [SnippetArtifact] (encoded with
 * [SnippetArtifactCodec]) to [ScriptingConfigurationKeys.REPL_SNIPPET_ARTIFACT_OUTPUT].
 *
 * This is a **compile-only** path: it produces a portable artifact and performs no evaluation, so
 * the same invocation works from the CLI and from a regular `CompileService.compile(...)` (the
 * daemon forwards plugin args verbatim) without any REPL-specific transport. A clean compile writes
 * the artifact and returns [ExitCode.OK]; any error diagnostic yields [ExitCode.COMPILATION_ERROR]
 * and no artifact is written (so a written output always implies a clean snippet compile).
 */
internal fun compileReplSnippet(
    snippet: SourceCode,
    configuration: CompilerConfiguration,
): ExitCode {
    val outputFile = configuration.get(ScriptingConfigurationKeys.REPL_SNIPPET_ARTIFACT_OUTPUT)
    if (outputFile == null) {
        configuration.report(
            ScriptDiagnostic.Severity.ERROR,
            "REPL snippet compilation mode requires an output artifact path " +
                    "(plugin option 'repl-snippet-artifact-output')",
            null,
        )
        return ExitCode.COMPILATION_ERROR
    }

    val priors: List<SnippetArtifact> = try {
        configuration.getList(ScriptingConfigurationKeys.REPL_SNIPPET_PRIOR_ARTIFACTS).map { file ->
            SnippetArtifactCodec.decode(file.readBytes())
        }
    } catch (t: Throwable) {
        configuration.report(
            ScriptDiagnostic.Severity.ERROR,
            "REPL snippet compilation: failed to read/decode a prior-snippet artifact: ${t.message}",
            null,
        )
        return ExitCode.COMPILATION_ERROR
    }

    val hostConfiguration = (configuration.scriptingHostConfiguration as? ScriptingHostConfiguration)
        ?: defaultJvmScriptingHostConfiguration

    // Stateless reconstruction keys snippets by their source name; when the source arrives through
    // `-expression` it is always synthetically named `script.kts`, which collides across a
    // multi-snippet sequence. An explicit `repl-snippet-name` (REPL_SNIPPET_NAME) lets the caller
    // assign a distinct, deterministic name per snippet so its priors resolve correctly.
    val explicitName = configuration.get(ScriptingConfigurationKeys.REPL_SNIPPET_NAME)
    val effectiveSnippet: SourceCode =
        if (explicitName != null && explicitName != snippet.name) StringScriptSource(snippet.text, explicitName)
        else snippet

    val classpath = configuration.jvmClasspathRoots
    val scriptCompilationConfiguration = ScriptCompilationConfiguration {
        if (classpath.isNotEmpty()) {
            updateClasspath(classpath)
        }
    }

    @Suppress("DEPRECATION_ERROR")
    val result: ResultWithDiagnostics<SnippetArtifact> = internalScriptingRunSuspend {
        K2ReplStatelessCompiler().compile(
            priorSnippets = priors,
            snippet = effectiveSnippet,
            scriptCompilationConfiguration = scriptCompilationConfiguration,
            hostConfiguration = hostConfiguration,
        )
    }

    val sourceLines = if (result.reports.isEmpty()) null else runCatching { effectiveSnippet.text.lines() }.getOrNull()
    for (report in result.reports) {
        val location = report.location
        val sourcePath = report.sourcePath
        configuration.report(
            report.severity,
            report.render(withSeverity = false, withLocation = location == null || sourcePath == null),
            if (location != null && sourcePath != null) {
                CompilerMessageLocation.create(
                    sourcePath,
                    location.start.line, location.start.col,
                    sourceLines?.getOrNull(location.start.line - 1),
                )
            } else null,
        )
    }

    val hasErrors = result.reports.any {
        it.severity == ScriptDiagnostic.Severity.ERROR || it.severity == ScriptDiagnostic.Severity.FATAL
    }
    return when (result) {
        is ResultWithDiagnostics.Success -> {
            if (hasErrors) {
                // Best-effort artifacts (codegen succeeded despite FIR errors) are not persisted on
                // this path: a written output always implies a clean snippet compile.
                ExitCode.COMPILATION_ERROR
            } else {
                try {
                    outputFile.parentFile?.mkdirs()
                    outputFile.writeBytes(SnippetArtifactCodec.encode(result.value))
                } catch (t: Throwable) {
                    configuration.report(
                        ScriptDiagnostic.Severity.ERROR,
                        "REPL snippet compilation: failed to write output artifact to $outputFile: ${t.message}",
                        null,
                    )
                    return ExitCode.COMPILATION_ERROR
                }
                ExitCode.OK
            }
        }
        is ResultWithDiagnostics.Failure -> ExitCode.COMPILATION_ERROR
    }
}

open class ExplicitlyNamedFileScriptSource(
    override val name: String, file: File, preloadedText: String? = null
) : FileScriptSource(file, preloadedText), Serializable {

    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }
}
