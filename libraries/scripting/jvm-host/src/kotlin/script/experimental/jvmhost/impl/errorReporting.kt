/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asErrorDiagnostics

internal class ScriptDiagnosticsMessageCollector : MessageCollector {

    private val _diagnostics = arrayListOf<ScriptDiagnostic>()

    val diagnostics: List<ScriptDiagnostic> get() = _diagnostics

    override fun clear() {
        _diagnostics.clear()
    }

    override fun hasErrors(): Boolean =
        _diagnostics.any { it.severity == ScriptDiagnostic.Severity.ERROR }


    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        val mappedSeverity = when (severity) {
            CompilerMessageSeverity.EXCEPTION,
            CompilerMessageSeverity.ERROR -> ScriptDiagnostic.Severity.ERROR
            CompilerMessageSeverity.STRONG_WARNING,
            CompilerMessageSeverity.WARNING -> ScriptDiagnostic.Severity.WARNING
            CompilerMessageSeverity.INFO -> ScriptDiagnostic.Severity.INFO
            CompilerMessageSeverity.LOGGING -> ScriptDiagnostic.Severity.DEBUG
            else -> null
        }
        if (mappedSeverity != null) {
            val mappedLocation = location?.let {
                if (it.line < 0 && it.column < 0) null // special location created by CompilerMessageLocation.create
                else SourceCode.Location(
                    SourceCode.Position(
                        it.line,
                        it.column
                    )
                )
            }
            _diagnostics.add(ScriptDiagnostic(message, mappedSeverity, location?.path, mappedLocation))
        }
    }
}

internal fun failure(
    messageCollector: ScriptDiagnosticsMessageCollector, vararg diagnostics: ScriptDiagnostic
): ResultWithDiagnostics.Failure =
    ResultWithDiagnostics.Failure(*messageCollector.diagnostics.toTypedArray(), *diagnostics)

internal fun failure(
    script: SourceCode, messageCollector: ScriptDiagnosticsMessageCollector, message: String
): ResultWithDiagnostics.Failure =
    failure(messageCollector, message.asErrorDiagnostics(path = script.locationId))

internal class IgnoredOptionsReportingState {
    var currentArguments = K2JVMCompilerArguments()
}

internal fun reportArgumentsIgnoredGenerally(
    arguments: K2JVMCompilerArguments,
    messageCollector: MessageCollector,
    reportingState: IgnoredOptionsReportingState
) {

    reportIgnoredArguments(
        arguments,
        "The following compiler arguments are ignored on script compilation: ",
        messageCollector,
        reportingState,
        K2JVMCompilerArguments::version,
        K2JVMCompilerArguments::destination,
        K2JVMCompilerArguments::buildFile,
        K2JVMCompilerArguments::commonSources,
        K2JVMCompilerArguments::allWarningsAsErrors,
        K2JVMCompilerArguments::script,
        K2JVMCompilerArguments::scriptTemplates,
        K2JVMCompilerArguments::scriptResolverEnvironment,
        K2JVMCompilerArguments::disableStandardScript,
        K2JVMCompilerArguments::disableDefaultScriptingPlugin,
        K2JVMCompilerArguments::pluginClasspaths,
        K2JVMCompilerArguments::pluginOptions,
        K2JVMCompilerArguments::useJavac,
        K2JVMCompilerArguments::compileJava,
        K2JVMCompilerArguments::reportPerf,
        K2JVMCompilerArguments::dumpPerf
    )
}

internal fun reportArgumentsIgnoredFromRefinement(
    arguments: K2JVMCompilerArguments, messageCollector: MessageCollector, reportingState: IgnoredOptionsReportingState
) {
    reportIgnoredArguments(
        arguments,
        "The following compiler arguments are ignored when configured from refinement callbacks: ",
        messageCollector,
        reportingState,
        K2JVMCompilerArguments::noJdk,
        K2JVMCompilerArguments::jdkHome,
        K2JVMCompilerArguments::javaModulePath,
        K2JVMCompilerArguments::classpath,
        K2JVMCompilerArguments::noStdlib,
        K2JVMCompilerArguments::noReflect
    )
}

private fun reportIgnoredArguments(
    arguments: K2JVMCompilerArguments, message: String,
    messageCollector: MessageCollector, reportingState: IgnoredOptionsReportingState,
    vararg toIgnore: KMutableProperty1<K2JVMCompilerArguments, *>
) {
    val ignoredArgKeys = toIgnore.mapNotNull { argProperty ->
        if (argProperty.get(arguments) != argProperty.get(reportingState.currentArguments)) {
            argProperty.findAnnotation<Argument>()?.value
                ?: throw IllegalStateException("unknown compiler argument property: $argProperty: no Argument annotation found")
        } else null
    }

    if (ignoredArgKeys.isNotEmpty()) {
        messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, "$message${ignoredArgKeys.joinToString(", ")}")
    }
}