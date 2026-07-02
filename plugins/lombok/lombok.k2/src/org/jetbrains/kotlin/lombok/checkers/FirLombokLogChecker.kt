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
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.isLogger
import org.jetbrains.kotlin.lombok.generators.kotlin.isRelevantForConflictsCheck
import org.jetbrains.kotlin.name.Name

object FirLombokLogChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val lombokService = context.session.lombokService
        val logs = lombokService.getLogs(declaration.symbol).takeIf { it.isNotEmpty() } ?: return

        val container = if (lombokService.config.logFieldIsStatic) {
            if (declaration.isCompanion) {
                declaration.symbol
            } else {
                declaration.companionObjectSymbol ?: return
            }
        } else {
            declaration.symbol
        }

        val fieldName = Name.identifier(lombokService.config.logFieldName)
        val declaredMemberScope = context.session.declaredMemberScope(container, memberRequiredPhase = null)

        for (log in logs) {
            var hasConflict = false
            declaredMemberScope.processPropertiesByName(fieldName) {
                if (it.origin.isLogger(log.annotation)) {
                    // Check that the return type and annotations are resolved to prevent crashing on codegen.
                    if (it.resolvedReturnType.toSymbol(context.session) == null) {
                        reporter.reportOn(log.annotation.source, FirErrors.MISSING_DEPENDENCY_CLASS, it.resolvedReturnType, context)
                    }
                    for (annotation in it.resolvedAnnotationsWithArguments) {
                        if (annotation.annotationTypeRef.toRegularClassSymbol(context.session) == null) {
                            reporter.reportOn(
                                log.annotation.source,
                                FirErrors.MISSING_DEPENDENCY_CLASS,
                                annotation.annotationTypeRef.coneType,
                                context
                            )
                        }
                    }
                } else {
                    hasConflict = hasConflict || it.isRelevantForConflictsCheck
                }
            }
            if (!hasConflict) continue

            // Mirrors the Java Lombok behavior: "Field 'log' already exists."
            reporter.reportOn(log.annotation.source, LombokFirDiagnostics.LOG_PROPERTY_ALREADY_EXISTS, fieldName, context)
        }
    }
}
