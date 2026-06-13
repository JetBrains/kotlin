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
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.k2.config.FlagUsageValue
import org.jetbrains.kotlin.lombok.k2.config.lombokService

object FirLombokUsageChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val lombokService = context.session.lombokService

        val lombokAnnotationsWithFlagUsages = buildList {
            lombokService.getLogs(declaration.symbol).forEach { log ->
                val specificFlagUsage = when (log) {
                    is ConeLombokAnnotations.Log -> lombokService.config.javaUtilLogFlagUsage
                    is ConeLombokAnnotations.Slf4jLog -> lombokService.config.slf4jLogFlagUsage
                    is ConeLombokAnnotations.Log4jLog -> lombokService.config.log4jLogFlagUsage
                    is ConeLombokAnnotations.CommonsLog -> lombokService.config.commonsLogFlagUsage
                    is ConeLombokAnnotations.FloggerLog -> lombokService.config.floggerLogFlagUsage
                    is ConeLombokAnnotations.JBossLog -> lombokService.config.jbossLogFlagUsage
                    is ConeLombokAnnotations.Log4j2Log -> lombokService.config.log4j2LogFlagUsage
                    is ConeLombokAnnotations.XSlf4jLog -> lombokService.config.xslf4jLogFlagUsage
                }
                val maxOrdinal = maxOf(
                    specificFlagUsage?.ordinal ?: -1,
                    lombokService.config.logFlagUsage?.ordinal ?: -1
                )
                if (maxOrdinal >= 0) {
                    add(log to FlagUsageValue.entries.single { it.ordinal == maxOrdinal })
                }
            }
            lombokService.config.toStringFlagUsage?.let { toStringFlagUsage ->
                lombokService.getToString(declaration.symbol)?.let { toString ->
                    add(toString to toStringFlagUsage)
                }
            }
        }

        for ([actualLombokAnnotation, flagUsage] in lombokAnnotationsWithFlagUsages) {
            val source = actualLombokAnnotation.annotation.source ?: declaration.source ?: continue
            val factory = when (flagUsage) {
                FlagUsageValue.Warning -> LombokFirDiagnostics.FLAG_USAGE_WARNING
                FlagUsageValue.Error -> LombokFirDiagnostics.FLAG_USAGE_ERROR
            }
            reporter.reportOn(
                source,
                factory,
                actualLombokAnnotation.annotation.toAnnotationClassId(context.session)!!.shortClassName,
                context
            )
        }
    }
}
