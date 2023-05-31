/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.scripting.definitions.MessageReporter
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaField
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asErrorDiagnostics
import kotlin.script.experimental.jvm.util.toSourceCodePosition

class ScriptDiagnosticsMessageCollector(private val parentMessageCollector: MessageCollector?) : MessageCollector {

    private val _diagnostics = arrayListOf<ScriptDiagnostic>()

    val diagnostics: List<ScriptDiagnostic> get() = _diagnostics

    override fun clear() {
        _diagnostics.clear()
        parentMessageCollector?.clear()
    }

    override fun hasErrors(): Boolean =
        _diagnostics.any { it.severity == ScriptDiagnostic.Severity.ERROR } || parentMessageCollector?.hasErrors() == true

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val mappedSeverity = severity.toScriptingSeverity()
        if (mappedSeverity != null) {
            val mappedLocation = location?.let {
                if (it.line < 0 && it.column < 0) null // special location created by CompilerMessageLocation.create
                else if (it.lineEnd < 0 && it.columnEnd < 0) SourceCode.Location(
                    SourceCode.Position(
                        it.line,
                        it.column
                    )
                )
                else SourceCode.Location(
                    SourceCode.Position(
                        it.line,
                        it.column
                    ),
                    SourceCode.Position(
                        it.lineEnd,
                        it.columnEnd
                    )
                )
            }
            _diagnostics.add(ScriptDiagnostic(ScriptDiagnostic.unspecifiedError, message, mappedSeverity, location?.path, mappedLocation))
        }
        parentMessageCollector?.report(severity, message, location)
    }

    fun report(diagnostic: ScriptDiagnostic) {
        _diagnostics.add(diagnostic)

        if (parentMessageCollector == null) return
        if (parentMessageCollector is ScriptDiagnosticsMessageCollector) {
            parentMessageCollector.report(diagnostic)
            return
        }

        val locationStart = diagnostic.location?.start
        parentMessageCollector.report(
            diagnostic.severity.toCompilerMessageSeverity(),
            diagnostic.message,
            CompilerMessageLocation.create(
                null,
                locationStart?.line ?: -1,
                locationStart?.col ?: -1,
                null
            )
        )
    }
}

private fun CompilerMessageSeverity.toScriptingSeverity(): ScriptDiagnostic.Severity? = when (this) {
    CompilerMessageSeverity.EXCEPTION,
    CompilerMessageSeverity.ERROR -> ScriptDiagnostic.Severity.ERROR
    CompilerMessageSeverity.STRONG_WARNING,
    CompilerMessageSeverity.WARNING -> ScriptDiagnostic.Severity.WARNING
    CompilerMessageSeverity.INFO -> ScriptDiagnostic.Severity.INFO
    CompilerMessageSeverity.LOGGING -> ScriptDiagnostic.Severity.DEBUG
    CompilerMessageSeverity.OUTPUT -> null
}

private fun ScriptDiagnostic.Severity.toCompilerMessageSeverity(): CompilerMessageSeverity = when (this) {
    ScriptDiagnostic.Severity.ERROR -> CompilerMessageSeverity.ERROR
    ScriptDiagnostic.Severity.WARNING -> CompilerMessageSeverity.WARNING
    ScriptDiagnostic.Severity.INFO -> CompilerMessageSeverity.INFO
    ScriptDiagnostic.Severity.DEBUG -> CompilerMessageSeverity.LOGGING
    ScriptDiagnostic.Severity.FATAL -> CompilerMessageSeverity.EXCEPTION
}

fun failure(
    messageCollector: ScriptDiagnosticsMessageCollector, vararg diagnostics: ScriptDiagnostic
): ResultWithDiagnostics.Failure =
    ResultWithDiagnostics.Failure(*messageCollector.diagnostics.toTypedArray(), *diagnostics)

fun failure(
    script: SourceCode, messageCollector: ScriptDiagnosticsMessageCollector, message: String
): ResultWithDiagnostics.Failure =
    failure(messageCollector, message.asErrorDiagnostics(path = script.locationId))

class IgnoredOptionsReportingState {
    var currentArguments = K2JVMCompilerArguments()
}

internal fun reportArgumentsNotAllowed(
    arguments: K2JVMCompilerArguments,
    messageCollector: MessageCollector,
    reportingState: IgnoredOptionsReportingState
) =
    reportInvalidArguments(
        arguments,
        "The following compiler arguments are not allowed in the script compilation configuration: ",
        CompilerMessageSeverity.ERROR,
        messageCollector,
        reportingState,
        K2JVMCompilerArguments::useJavac,
        K2JVMCompilerArguments::useK2
    )

internal fun reportArgumentsIgnoredGenerally(
    arguments: K2JVMCompilerArguments,
    messageCollector: MessageCollector,
    reportingState: IgnoredOptionsReportingState
) =
    reportInvalidArguments(
        arguments,
        "The following compiler arguments are ignored on script compilation: ",
        CompilerMessageSeverity.STRONG_WARNING,
        messageCollector,
        reportingState,
        K2JVMCompilerArguments::version,
        K2JVMCompilerArguments::destination,
        K2JVMCompilerArguments::buildFile,
        K2JVMCompilerArguments::commonSources,
        K2JVMCompilerArguments::allWarningsAsErrors,
        K2JVMCompilerArguments::script,
        K2JVMCompilerArguments::expression,
        K2JVMCompilerArguments::scriptTemplates,
        K2JVMCompilerArguments::scriptResolverEnvironment,
        K2JVMCompilerArguments::disableStandardScript,
        K2JVMCompilerArguments::defaultScriptExtension,
        K2JVMCompilerArguments::disableDefaultScriptingPlugin,
        K2JVMCompilerArguments::useJavac,
        K2JVMCompilerArguments::compileJava,
        K2JVMCompilerArguments::reportPerf,
        K2JVMCompilerArguments::dumpPerf
    )

internal fun reportArgumentsIgnoredFromRefinement(
    arguments: K2JVMCompilerArguments, messageCollector: MessageCollector, reportingState: IgnoredOptionsReportingState
) =
    reportInvalidArguments(
        arguments,
        "The following compiler arguments are ignored when configured from refinement callbacks: ",
        CompilerMessageSeverity.STRONG_WARNING,
        messageCollector,
        reportingState,
        K2JVMCompilerArguments::noJdk,
        K2JVMCompilerArguments::jdkHome,
        K2JVMCompilerArguments::javaModulePath,
        K2JVMCompilerArguments::classpath,
        K2JVMCompilerArguments::noStdlib,
        K2JVMCompilerArguments::noReflect,
        K2JVMCompilerArguments::pluginClasspaths,
        K2JVMCompilerArguments::pluginOptions,
        K2JVMCompilerArguments::pluginConfigurations,
    )


private fun reportInvalidArguments(
    arguments: K2JVMCompilerArguments,
    message: String, severity: CompilerMessageSeverity,
    messageCollector: MessageCollector, reportingState: IgnoredOptionsReportingState,
    vararg toIgnore: KMutableProperty1<K2JVMCompilerArguments, *>
): Boolean {
    val invalidArgKeys = toIgnore.mapNotNull { argProperty ->
        if (argProperty.get(arguments) != argProperty.get(reportingState.currentArguments)) {
            argProperty.javaField?.getAnnotation(Argument::class.java)?.value
                ?: throw IllegalStateException("unknown compiler argument property: $argProperty: no Argument annotation found")
        } else null
    }

    if (invalidArgKeys.isNotEmpty()) {
        messageCollector.report(severity, "$message${invalidArgKeys.joinToString(", ")}")
        return true
    }
    return false
}

val MessageCollector.reporter: MessageReporter
    get() = { severity, message ->
        this.report(severity.toCompilerMessageSeverity(), message)
    }

fun KtDiagnostic.asScriptDiagnostic(sourceCode: SourceCode): ScriptDiagnostic {
    val (diagnosticCode, scriptSeverity) = when (severity) {
        Severity.INFO -> ScriptDiagnostic.unspecifiedInfo to ScriptDiagnostic.Severity.INFO
        Severity.ERROR -> ScriptDiagnostic.unspecifiedError to ScriptDiagnostic.Severity.ERROR
        Severity.WARNING -> ScriptDiagnostic.unspecifiedInfo to ScriptDiagnostic.Severity.WARNING
    }

    val renderer = RootDiagnosticRendererFactory(this)

    val location = if (textRanges.isEmpty()) {
        null
    } else {
        val firstRange = textRanges.first()
        val lastRange = textRanges.last()
        SourceCode.LocationWithId(
            element.psi?.containingFile?.virtualFile?.path.orEmpty(),
            SourceCode.Location(
                firstRange.startOffset.toSourceCodePosition(sourceCode),
                lastRange.endOffset.toSourceCodePosition(sourceCode)
            )
        )
    }

    return ScriptDiagnostic(
        diagnosticCode,
        renderer.render(this),
        scriptSeverity,
        location
    )
}

fun BaseDiagnosticsCollector.scriptDiagnostics(sourceCode: SourceCode) = diagnostics.map {
    it.asScriptDiagnostic(sourceCode)
}
