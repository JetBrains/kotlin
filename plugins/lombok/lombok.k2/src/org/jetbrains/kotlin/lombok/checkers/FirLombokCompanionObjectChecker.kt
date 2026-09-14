/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.processAllClassifiers
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.generators.LombokCompanionObjectContributor
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT

/**
 * Reports a nested classifier that takes the name of the companion object Lombok would otherwise generate.
 *
 * `@Log`, `@Builder` and a `staticName`-bearing constructor annotation all put what they generate into the
 * containing class's companion object, and `isCompanionNeeded` refuses to generate one whose name is already
 * taken - a nested `class Companion` is a classifier like any other, and the generated companion object clashed
 * with it (KT-88276). Nothing is generated for such a class, and this is what says so: without it the class is
 * left silently missing its logger or its `builder()`, and every use site is a bare `UNRESOLVED_REFERENCE`.
 *
 * The report goes on the classifier holding the name rather than on the annotations that wanted the companion
 * object: it is the one thing to change, it is where `REDECLARATION` used to point, and it is reported once
 * however many annotations the class carries.
 */
object FirLombokCompanionObjectChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        // A class that declares a companion object, whatever its name, has somewhere to put the static members
        // already, so nothing is blocked here.
        if (declaration.companionObjectSymbol != null) return

        var nameHolder: FirClassLikeSymbol<*>? = null
        declaration.symbol.processAllClassifiers(context.session) { classifier ->
            // The scope includes generated classifiers, so the companion object this plugin generates for a
            // class that has room for one shows up here as well - and being a companion object, it is exactly
            // what nothing is blocked by.
            if (nameHolder == null &&
                classifier is FirClassLikeSymbol<*> &&
                !classifier.isCompanion &&
                classifier.name == DEFAULT_NAME_FOR_COMPANION_OBJECT
            ) {
                nameHolder = classifier
            }
        }
        val occupiedBy = nameHolder ?: return

        // The generators decide what needs a companion object; asking them keeps this from re-deriving which
        // annotation on which declaration generates a static member.
        val needsCompanionObject = context.session.extensionService.declarationGenerators.any {
            it is LombokCompanionObjectContributor && it.needsCompanionObject(declaration.symbol)
        }
        if (!needsCompanionObject) return

        reporter.reportOn(occupiedBy.source, LombokFirDiagnostics.COMPANION_OBJECT_IS_NOT_GENERATED, context)
    }
}
