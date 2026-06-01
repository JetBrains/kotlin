/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.k2.config.lombokService

object FirLombokConstructorsChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val toStringAnnInfo = context.session.lombokService.getNoArgsConstructor(declaration.symbol) ?: return
        if (toStringAnnInfo.force) return
        val source = toStringAnnInfo.annotation.source ?: declaration.source ?: return

        val declaredMemberScope = context.session.declaredMemberScope(declaration.symbol, memberRequiredPhase = null)

        var hasUninitializedValProperty = false
        declaredMemberScope.processAllProperties { varSymbol ->
            hasUninitializedValProperty = hasUninitializedValProperty ||
                    (varSymbol.isVal &&
                            varSymbol.resolvedInitializer.let { it == null || it.source?.kind is KtFakeSourceElementKind } &&
                            (varSymbol as? FirPropertySymbol)?.hasBackingField == true)
        }

        if (hasUninitializedValProperty) {
            reporter.reportOn(source, LombokFirDiagnostics.NO_ARGS_CONSTRUCTOR_FORCE_REQUIRED)
        }
    }
}
