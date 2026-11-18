/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.LOMBOK_GENERATED_DECLARATION_CLASH
import org.jetbrains.kotlin.lombok.generators.LombokDeclarationKey
import org.jetbrains.kotlin.lombok.generators.clashesByParametersWith

object FirLombokConflictingDeclarationsChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        check(declaration, declaration.source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun check(declaration: FirRegularClass, sourceToReport: KtSourceElement?) {
        val scope = declaration.unsubstitutedScope()
        val callableNames = scope.getCallableNames()
        for (name in callableNames) {
            val potentiallyConflictingGroups = mutableListOf<MutableList<FirNamedFunctionSymbol>>()
            scope.processFunctionsByName(name) { symbol ->
                val origin = symbol.origin
                if (origin !is FirDeclarationOrigin.Plugin || origin.key !is LombokDeclarationKey) return@processFunctionsByName
                var conflictingGroupFound = false
                for (group in potentiallyConflictingGroups) {
                    if (group.any { otherSymbol ->
                            symbol.clashesByParametersWith(context.session, otherSymbol)
                        }
                    ) {
                        group += symbol
                        conflictingGroupFound = true
                        break
                    }
                }
                if (!conflictingGroupFound) {
                    potentiallyConflictingGroups += mutableListOf(symbol)
                }
            }
            for (group in potentiallyConflictingGroups) {
                if (group.size <= 1) continue
                reporter.reportOn(
                    sourceToReport,
                    LOMBOK_GENERATED_DECLARATION_CLASH,
                    group,
                    context,
                )
            }
            val potentiallyConflictingVariables = mutableListOf<FirVariableSymbol<*>>()
            scope.processPropertiesByName(name) { symbol ->
                val origin = symbol.origin
                if (origin !is FirDeclarationOrigin.Plugin || origin.key !is LombokDeclarationKey) return@processPropertiesByName
                potentiallyConflictingVariables += symbol
            }
            if (potentiallyConflictingVariables.size > 1) {
                reporter.reportOn(
                    sourceToReport,
                    LOMBOK_GENERATED_DECLARATION_CLASH,
                    potentiallyConflictingVariables,
                    context,
                )
            }
        }
        for (nestedClassifier in declaration.generatedNestedClassifiers(context.session)) {
            val origin = nestedClassifier.origin
            if (nestedClassifier is FirRegularClass && origin is FirDeclarationOrigin.Plugin && origin.key is LombokDeclarationKey) {
                check(nestedClassifier, sourceToReport)
            }
        }
    }
}
