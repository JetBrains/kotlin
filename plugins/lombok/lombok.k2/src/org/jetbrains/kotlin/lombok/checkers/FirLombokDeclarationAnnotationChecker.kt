/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

/**
 * Validates the Lombok annotations written on a declaration.
 * See [FirLombokExpressionAnnotationChecker] for the ones written on an expression.
 */
object FirLombokDeclarationAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        // A Lombok annotation targeting `FIELD` (`@Builder.Default`, `@Singular` and the
        // `@ToString`/`@EqualsAndHashCode` `@Include`/`@Exclude` pairs), whether it is written with a
        // `@field:` use-site target or resolves to one implicitly, lands on the property's backing field. The
        // plugin acts on the property regardless of which of the two carries it.
        // The backing field's own list says only `BACKING_FIELD`, which names no shape the plugin knows.
        @OptIn(SymbolInternals::class)
        val targetOwner = (declaration as? FirBackingField)?.propertySymbol?.fir ?: declaration

        checkLombokAnnotations(
            declaration.annotations,
            getActualTargetList(targetOwner),
            targetOwner as? FirRegularClass,
        )
    }
}
