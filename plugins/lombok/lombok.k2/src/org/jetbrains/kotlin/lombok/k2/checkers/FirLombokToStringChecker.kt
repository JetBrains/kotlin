/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.ToString.CallSuperMode
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.k2.generators.ToStringGenerator
import org.jetbrains.kotlin.lombok.k2.generators.isRelevantForConflictsCheck
import org.jetbrains.kotlin.lombok.k2.generators.isToString
import org.jetbrains.kotlin.name.StandardClassIds


object FirLombokToStringChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val toStringAnnInfo = context.session.lombokService.getToString(declaration.symbol) ?: return
        val source = toStringAnnInfo.annotation.source ?: declaration.source ?: return

        val declaredMemberScope = context.session.declaredMemberScope(declaration.symbol, memberRequiredPhase = null)
        var hasConflict = false
        declaredMemberScope.processFunctionsByName(ToStringGenerator.TO_STRING_NAME) {
            hasConflict = hasConflict || it.isRelevantForConflictsCheck && !it.origin.isToString && it.valueParameterSymbols.isEmpty()
        }
        if (hasConflict) {
            /**
             * Mirrors the Java Lombok behaviour: "Not generating toString(): A method with that name already exists"
             */
            reporter.reportOn(source, LombokFirDiagnostics.TO_STRING_FUNCTION_ALREADY_EXISTS, context)
        }

        if (toStringAnnInfo.callSuper == CallSuperMode.Warn && declaration.hasNonAnyClassSupertype(context.session)) {
            /**
             * Mirrors Lombok Java behaviour: when `lombok.toString.callSuper=warn` is set and a `@ToString`-annotated
             * class has a non-trivial superclass (i.e. not just `kotlin.Any`/`java.lang.Object`), emits a warning
             * because `toString()` is generated without calling the superclass implementation.
             */
            reporter.reportOn(source, LombokFirDiagnostics.TO_STRING_CALL_SUPER_NOT_CALLED, context)
        }
    }

    private fun FirRegularClass.hasNonAnyClassSupertype(session: FirSession): Boolean {
        return superTypeRefs.any { ref ->
            val symbol = ref.toRegularClassSymbol(session) ?: return@any false
            symbol.classKind != ClassKind.INTERFACE && symbol.classId != StandardClassIds.Any
        }
    }
}
