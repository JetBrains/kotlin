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
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.k2.config.FlagUsageValue
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.utils.LombokNames

object FirLombokLogUsageChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val log = context.session.lombokService.getLog(declaration.symbol) ?: return
        val flagUsage = log.flagUsage ?: return
        val source = declaration.annotations.getAnnotationByClassId(LombokNames.LOG_ID, context.session)?.source
            ?: declaration.source
            ?: return
        val factory = when (flagUsage) {
            FlagUsageValue.Warning -> LombokFirDiagnostics.LOG_FLAG_USAGE_WARNING
            FlagUsageValue.Error -> LombokFirDiagnostics.LOG_FLAG_USAGE_ERROR
        }
        reporter.reportOn(source, factory, context)
    }
}
