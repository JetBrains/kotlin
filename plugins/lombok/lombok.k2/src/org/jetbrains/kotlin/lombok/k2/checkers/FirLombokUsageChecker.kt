/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.k2.config.FlagUsageValue
import org.jetbrains.kotlin.lombok.k2.config.lombokService

object FirLombokUsageChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val lombokService = context.session.lombokService

        val actualLombokAnnotation = lombokService.getLog(declaration.symbol) ?: lombokService.getToString(declaration.symbol) ?: return

        val flagUsage = actualLombokAnnotation.flagUsage ?: return
        val source = actualLombokAnnotation.annotation.source ?: declaration.source ?: return
        val factory = when (flagUsage) {
            FlagUsageValue.Warning -> LombokFirDiagnostics.FLAG_USAGE_WARNING
            FlagUsageValue.Error -> LombokFirDiagnostics.FLAG_USAGE_ERROR
        }
        reporter.reportOn(source, factory, actualLombokAnnotation.annotation.toAnnotationClassId(context.session)!!.shortClassName, context)
    }
}
