/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.legacy

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.script.ScriptReportSink
import kotlin.script.experimental.dependencies.ScriptReport

internal class CliScriptReportSink(private val messageCollector: MessageCollector) : ScriptReportSink {
    override fun attachReports(scriptFile: VirtualFile, reports: List<ScriptReport>) {
        reports.forEach {
            messageCollector.report(it.severity.convertSeverity(), it.message, location(scriptFile, it.position))
        }
    }

    private fun location(scriptFile: VirtualFile, position: ScriptReport.Position?): CompilerMessageLocation? {
        if (position == null) return CompilerMessageLocation.create(scriptFile.path)

        return CompilerMessageLocation.create(scriptFile.path, position.startLine, position.startColumn, null)
    }

    private fun ScriptReport.Severity.convertSeverity(): CompilerMessageSeverity = when (this) {
        ScriptReport.Severity.FATAL -> CompilerMessageSeverity.ERROR
        ScriptReport.Severity.ERROR -> CompilerMessageSeverity.ERROR
        ScriptReport.Severity.WARNING -> CompilerMessageSeverity.WARNING
        ScriptReport.Severity.INFO -> CompilerMessageSeverity.INFO
        ScriptReport.Severity.DEBUG -> CompilerMessageSeverity.LOGGING
    }
}
