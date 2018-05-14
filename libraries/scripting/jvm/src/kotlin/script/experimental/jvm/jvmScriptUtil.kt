/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvm

import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptSource
import kotlin.script.experimental.dependencies.ScriptReport

fun mapLegacyDiagnosticSeverity(severity: ScriptDependenciesResolver.ReportSeverity): ScriptDiagnostic.Severity = when (severity) {
    ScriptDependenciesResolver.ReportSeverity.FATAL -> ScriptDiagnostic.Severity.FATAL
    ScriptDependenciesResolver.ReportSeverity.ERROR -> ScriptDiagnostic.Severity.ERROR
    ScriptDependenciesResolver.ReportSeverity.WARNING -> ScriptDiagnostic.Severity.WARNING
    ScriptDependenciesResolver.ReportSeverity.INFO -> ScriptDiagnostic.Severity.INFO
    ScriptDependenciesResolver.ReportSeverity.DEBUG -> ScriptDiagnostic.Severity.DEBUG
}

fun mapToLegacyScriptReportSeverity(severity: ScriptDiagnostic.Severity): ScriptReport.Severity = when (severity) {
    ScriptDiagnostic.Severity.FATAL -> ScriptReport.Severity.FATAL
    ScriptDiagnostic.Severity.ERROR -> ScriptReport.Severity.ERROR
    ScriptDiagnostic.Severity.WARNING -> ScriptReport.Severity.WARNING
    ScriptDiagnostic.Severity.INFO -> ScriptReport.Severity.INFO
    ScriptDiagnostic.Severity.DEBUG -> ScriptReport.Severity.DEBUG
}

fun mapLegacyScriptPosition(pos: ScriptContents.Position?): ScriptSource.Location? =
    pos?.let { ScriptSource.Location(ScriptSource.Position(pos.line, pos.col)) }

fun mapToLegacyScriptReportPosition(pos: ScriptSource.Location?): ScriptReport.Position? =
    pos?.let { ScriptReport.Position(pos.start.line, pos.start.col) }
