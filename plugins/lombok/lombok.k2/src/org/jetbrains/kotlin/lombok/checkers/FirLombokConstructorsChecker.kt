/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.name.JvmStandardClassIds

object FirLombokConstructorsChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val toStringAnnInfo = context.session.lombokService.getNoArgsConstructor(declaration.symbol) ?: return

        val source = toStringAnnInfo.annotation.source ?: declaration.source ?: return

        if (!toStringAnnInfo.force) {
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

        if (toStringAnnInfo.staticName != null) {
            // If `staticName` is provided, we generate a companion object with a constructor function marked with `@JvmStatic`.
            // So, we need to check if it's resolved to prevent crashing on codegen.
            if (context.session.symbolProvider.getClassLikeSymbolByClassId(JvmStandardClassIds.Annotations.JvmStatic) == null) {
                reporter.reportOn(
                    source,
                    FirErrors.MISSING_DEPENDENCY_CLASS,
                    JvmStandardClassIds.Annotations.JvmStatic.createConeType(context.session),
                    context
                )
            }
        }
    }
}
