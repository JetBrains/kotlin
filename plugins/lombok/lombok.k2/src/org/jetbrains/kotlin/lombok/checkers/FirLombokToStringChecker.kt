/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.builtins.StandardNames.TO_STRING_NAME
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.isToString
import org.jetbrains.kotlin.lombok.generators.hasReceiverOrContextParameters

object FirLombokToStringChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    private val functionNames = setOf(TO_STRING_NAME)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val toStringAnnInfo = context.session.lombokService.getToString(declaration.symbol) ?: return
        val source = toStringAnnInfo.annotation.source ?: declaration.source ?: return

        val declaredMemberScope = context.session.declaredMemberScope(declaration.symbol, memberRequiredPhase = null)
        var hasConflict = false
        declaredMemberScope.processFunctionsByName(TO_STRING_NAME) {
            hasConflict = hasConflict || !it.hasReceiverOrContextParameters && !it.origin.isToString && it.valueParameterSymbols.isEmpty()
        }
        if (hasConflict) {
            /**
             * Mirrors the Java Lombok behavior: "Not generating toString(): A method with that name already exists"
             */
            reporter.reportOn(source, LombokFirDiagnostics.TO_STRING_FUNCTION_ALREADY_EXISTS, context)
        } else {
            /**
             * Mirrors javac's reaction to the `toString()` Lombok generates in this case:
             * "toString() in Child cannot override toString() in Parent; overridden method is final".
             * Only relevant when no conflicting `toString()` is declared, otherwise nothing is generated at all.
             */
            declaration.findSuperclassWithFinalToString()?.let { superClassSymbol ->
                reporter.reportOn(
                    source,
                    LombokFirDiagnostics.TO_STRING_FUNCTION_IS_FINAL_IN_SUPERCLASS,
                    superClassSymbol.name,
                    context,
                )
            }
        }

        checkCallSuper(
            toStringAnnInfo.callSuper ?: context.session.lombokService.config.toStringCallSuper,
            toStringAnnInfo,
            declaration,
            functionNames,
        )

        checkIncludeAndExcludeAnnotations(
            declaredMemberScope,
            LombokNames.TO_STRING_ID,
            toStringAnnInfo.onlyExplicitlyIncluded ?: context.session.lombokService.config.toStringOnlyExplicitlyIncluded,
        )
    }

    /** The closest superclass declaring a final parameterless `toString()`, if any. */
    context(context: CheckerContext)
    private fun FirRegularClass.findSuperclassWithFinalToString(): FirRegularClassSymbol? =
        findSuperclassWithFinalFunction(functionNames) {
            it.valueParameterSymbols.isEmpty() && !it.hasReceiverOrContextParameters
        }
}
