/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.definitions

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode

internal class CliScriptReportSink(private val messageCollector: MessageCollector) :
    ScriptReportSink {
    override fun attachReports(scriptFile: VirtualFile, reports: List<ScriptDiagnostic>) {
        reports.forEach {
            messageCollector.report(it.severity.convertSeverity(), it.message, location(scriptFile, it.location))
        }
    }

    private fun location(scriptFile: VirtualFile, location: SourceCode.Location?): CompilerMessageLocation? {
        if (location == null) return CompilerMessageLocation.create(scriptFile.path)

        return CompilerMessageLocation.create(scriptFile.path, location.start.line, location.start.col, null)
    }

    private fun ScriptDiagnostic.Severity.convertSeverity(): CompilerMessageSeverity = when (this) {
        ScriptDiagnostic.Severity.FATAL -> CompilerMessageSeverity.ERROR
        ScriptDiagnostic.Severity.ERROR -> CompilerMessageSeverity.ERROR
        ScriptDiagnostic.Severity.WARNING -> CompilerMessageSeverity.WARNING
        ScriptDiagnostic.Severity.INFO -> CompilerMessageSeverity.INFO
        ScriptDiagnostic.Severity.DEBUG -> CompilerMessageSeverity.LOGGING
    }
}
