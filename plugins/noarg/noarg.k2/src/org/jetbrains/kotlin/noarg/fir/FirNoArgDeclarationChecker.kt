/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol

object FirNoArgDeclarationChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        if (declaration.classKind != ClassKind.CLASS) return
        val matcher = context.session.noArgPredicateMatcher
        if (!matcher.isAnnotated(declaration.symbol)) return

        when {
            declaration.isInner -> reporter.reportOn(source, KtErrorsNoArg.NOARG_ON_INNER_CLASS_ERROR, context)
            declaration.isLocal -> reporter.reportOn(source, KtErrorsNoArg.NOARG_ON_LOCAL_CLASS_ERROR, context)
        }

        val superClassSymbol = declaration.symbol.getSuperClassSymbolOrAny(context.session)
        if (superClassSymbol.declarationSymbols.filterIsInstance<FirConstructorSymbol>().none { it.isNoArgConstructor() } && !matcher.isAnnotated(superClassSymbol)) {
            reporter.reportOn(source, KtErrorsNoArg.NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS, context)
        }

    }

    private fun FirConstructorSymbol.isNoArgConstructor(): Boolean {
        return valueParameterSymbols.all { it.hasDefaultValue }
    }
}
