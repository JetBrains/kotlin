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
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassIds
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.k2.config.FlagUsageValue
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

object FirLombokUsageChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    private val checkUsageAnnotations: Set<ClassId> = setOf(
        LombokNames.LOG_ID,
        LombokNames.TO_STRING_ID,
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val checkUsageAnnotation = declaration.annotations.getAnnotationByClassIds(checkUsageAnnotations, context.session) ?: return
        val classId = checkUsageAnnotation.toAnnotationClassId(context.session) ?: return
        val lombokService = context.session.lombokService

        val lombokAnnotation = when (classId) {
            LombokNames.LOG_ID -> lombokService.getLog(declaration.symbol)!!
            LombokNames.TO_STRING_ID -> lombokService.getToString(declaration.symbol)!!
            else -> shouldNotBeCalled()
        }

        val flagUsage = lombokAnnotation.flagUsage ?: return
        val source = checkUsageAnnotation.source
            ?: declaration.source
            ?: return
        val factory = when (flagUsage) {
            FlagUsageValue.Warning -> LombokFirDiagnostics.FLAG_USAGE_WARNING
            FlagUsageValue.Error -> LombokFirDiagnostics.FLAG_USAGE_ERROR
        }
        reporter.reportOn(source, factory, classId.shortClassName, context)
    }
}
