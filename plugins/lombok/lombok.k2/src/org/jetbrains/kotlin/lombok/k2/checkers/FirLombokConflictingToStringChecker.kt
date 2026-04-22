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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.k2.generators.ToStringGenerator
import org.jetbrains.kotlin.lombok.k2.generators.isRelevantForConflictsCheck
import org.jetbrains.kotlin.lombok.k2.generators.isToString
import org.jetbrains.kotlin.lombok.utils.LombokNames

/**
 * Mirrors the Java Lombok behaviour: "Not generating toString(): A method with that name already exists"
 */
object FirLombokConflictingToStringChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val source = declaration.annotations.getAnnotationByClassId(LombokNames.TO_STRING_ID, context.session)?.source ?: return

        val declaredMemberScope = context.session.declaredMemberScope(declaration.symbol, memberRequiredPhase = null)
        var hasConflict = false
        declaredMemberScope.processFunctionsByName(ToStringGenerator.TO_STRING_NAME) {
            hasConflict = hasConflict || it.isRelevantForConflictsCheck && !it.origin.isToString && it.valueParameterSymbols.isEmpty()
        }
        if (!hasConflict) return

        reporter.reportOn(source, LombokFirDiagnostics.TO_STRING_FUNCTION_ALREADY_EXISTS, context)
    }
}
