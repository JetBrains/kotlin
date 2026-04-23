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
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.k2.generators.isLogger
import org.jetbrains.kotlin.lombok.k2.generators.kotlin.isRelevantForConflictsCheck
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.Name

/**
 * Reports a warning when a `@Log`-annotated class already has a property whose name matches the
 * logger property name (default: `log`, configurable via `lombok.log.fieldName` in `lombok.config`).
 * In this situation the Lombok generator skips property generation, so the user needs to be
 * informed that their annotation has no effect.
 *
 * Mirrors the Java Lombok behaviour: "Field 'log' already exists."
 */
object FirLombokConflictingLogFieldChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val log = context.session.lombokService.getLog(declaration.symbol) ?: return

        val container = if (log.fieldIsStatic) {
            if (declaration.isCompanion) {
                declaration.symbol
            } else {
                declaration.companionObjectSymbol ?: return
            }
        } else {
            declaration.symbol
        }

        val fieldName = Name.identifier(log.fieldName)
        val declaredMemberScope = context.session.declaredMemberScope(container, memberRequiredPhase = null)
        var hasConflict = false
        declaredMemberScope.processPropertiesByName(fieldName) {
            hasConflict = hasConflict || it.isRelevantForConflictsCheck && !it.origin.isLogger
        }
        if (!hasConflict) return

        val source = declaration.annotations.getAnnotationByClassId(LombokNames.LOG_ID, context.session)!!.source ?: return

        reporter.reportOn(source, LombokFirDiagnostics.LOG_PROPERTY_ALREADY_EXISTS, fieldName, context)
    }
}
