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
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.config.lombokService

object FirLombokBuilderClassChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val lombokService = context.session.lombokService

        val classBuilder = lombokService.getBuilder(declaration.symbol) ?: lombokService.getSuperBuilder(declaration.symbol)
        if (classBuilder != null && declaration.isBuilderCapableClass) {
            val primaryConstructor = declaration.primaryConstructorIfAny(context.session)
            if (primaryConstructor == null) {
                val annotationName = classBuilder.annotation.toAnnotationClassId(context.session)?.shortClassName
                if (annotationName != null) {
                    reporter.reportOn(
                        classBuilder.annotation.source,
                        LombokFirDiagnostics.BUILDER_REQUIRES_PRIMARY_CONSTRUCTOR,
                        annotationName,
                        context,
                    )
                }
            }
        }
    }
}
