/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.legacy.k1

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.report
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils.getLineAndColumnRange

/**
 * This class behaviour is the same as [org.jetbrains.kotlin.cli.common.messages.MessageCollector.report] in [org.jetbrains.kotlin.scripting.compiler.plugin.legacy.k1.AnalyzerWithCompilerReport.reportDiagnostic].
 */
@K1Deprecation
class DefaultDiagnosticReporter(override val messageCollector: MessageCollector) : MessageCollectorBasedReporter

@K1Deprecation
interface MessageCollectorBasedReporter : DiagnosticMessageReporter {
    val messageCollector: MessageCollector

    override fun report(diagnostic: Diagnostic, file: PsiFile, render: String) = messageCollector.report(
        diagnostic.severity.toCompilerMessageSeverity(),
        render,
        MessageUtil.psiFileToMessageLocation(file, file.name, getLineAndColumnRange(diagnostic.psiFile, diagnostic.textRanges)),
        diagnostic.factoryNameOrNull()
    )
}

@K1Deprecation
internal fun Diagnostic.factoryNameOrNull(): String? = factory.nameOrNull
