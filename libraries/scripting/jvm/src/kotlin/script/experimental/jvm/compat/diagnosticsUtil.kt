/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("unused")

package kotlin.script.experimental.jvm.compat

import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.dependencies.ScriptReport

fun mapLegacyDiagnosticSeverity(severity: ScriptDependenciesResolver.ReportSeverity): ScriptDiagnostic.Severity = when (severity) {
    ScriptDependenciesResolver.ReportSeverity.FATAL -> ScriptDiagnostic.Severity.FATAL
    ScriptDependenciesResolver.ReportSeverity.ERROR -> ScriptDiagnostic.Severity.ERROR
    ScriptDependenciesResolver.ReportSeverity.WARNING -> ScriptDiagnostic.Severity.WARNING
    ScriptDependenciesResolver.ReportSeverity.INFO -> ScriptDiagnostic.Severity.INFO
    ScriptDependenciesResolver.ReportSeverity.DEBUG -> ScriptDiagnostic.Severity.DEBUG
}

fun mapLegacyDiagnosticSeverity(severity: ScriptReport.Severity): ScriptDiagnostic.Severity = when (severity) {
    ScriptReport.Severity.FATAL -> ScriptDiagnostic.Severity.FATAL
    ScriptReport.Severity.ERROR -> ScriptDiagnostic.Severity.ERROR
    ScriptReport.Severity.WARNING -> ScriptDiagnostic.Severity.WARNING
    ScriptReport.Severity.INFO -> ScriptDiagnostic.Severity.INFO
    ScriptReport.Severity.DEBUG -> ScriptDiagnostic.Severity.DEBUG
}

fun mapToLegacyScriptReportSeverity(severity: ScriptDiagnostic.Severity): ScriptReport.Severity = when (severity) {
    ScriptDiagnostic.Severity.FATAL -> ScriptReport.Severity.FATAL
    ScriptDiagnostic.Severity.ERROR -> ScriptReport.Severity.ERROR
    ScriptDiagnostic.Severity.WARNING -> ScriptReport.Severity.WARNING
    ScriptDiagnostic.Severity.INFO -> ScriptReport.Severity.INFO
    ScriptDiagnostic.Severity.DEBUG -> ScriptReport.Severity.DEBUG
}

fun mapLegacyScriptPosition(pos: ScriptContents.Position?): SourceCode.Location? =
    pos?.let { SourceCode.Location(SourceCode.Position(pos.line, pos.col)) }

fun mapLegacyScriptPosition(pos: ScriptReport.Position?): SourceCode.Location? =
    pos?.let {
        val endPos =
            if (pos.endLine == null || pos.endColumn == null) null
            else SourceCode.Position(pos.endLine!!, pos.endColumn!!)
        SourceCode.Location(SourceCode.Position(pos.startLine, pos.startColumn), endPos)
    }

fun mapToLegacyScriptReportPosition(pos: SourceCode.Location?): ScriptReport.Position? =
    pos?.let { ScriptReport.Position(pos.start.line, pos.start.col, pos.end?.line, pos.end?.col) }

fun Iterable<ScriptReport>.mapToDiagnostics(): List<ScriptDiagnostic> = map { (message, severity, position) ->
    ScriptDiagnostic(message, mapLegacyDiagnosticSeverity(severity), null, mapLegacyScriptPosition(position))
}

fun Iterable<ScriptDiagnostic>.mapToLegacyReports(): List<ScriptReport> = map { (message, severity, _, location, exception) ->
    val reportMessage = if (exception == null) message else "$message ($exception)"
    ScriptReport(reportMessage, mapToLegacyScriptReportSeverity(severity), mapToLegacyScriptReportPosition(location))
}
