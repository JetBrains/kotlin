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
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.config.FlagUsageValue
import org.jetbrains.kotlin.lombok.config.lombokService

object FirLombokUsageChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
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
                louderFlagUsage(specificFlagUsage, lombokService.config.logFlagUsage)?.let { add(log to it) }
            }
            lombokService.config.toStringFlagUsage?.let { toStringFlagUsage ->
                lombokService.getToString(declaration.symbol)?.let { toString ->
                    add(toString to toStringFlagUsage)
                }
            }
            lombokService.config.equalsAndHashCodeFlagUsage?.let { equalsAndHashCodeFlagUsage ->
                lombokService.getEqualsAndHashCode(declaration.symbol)?.let { equalsAndHashCode ->
                    add(equalsAndHashCode to equalsAndHashCodeFlagUsage)
                }
            }
            lombokService.config.builderFlagUsage?.let { builderFlagUsage ->
                lombokService.getBuilder(declaration.symbol)?.let { builder ->
                    add(builder to builderFlagUsage)
                }
            }
            lombokService.config.superBuilderFlagUsage?.let { superBuilderFlagUsage ->
                declaration.annotations.getAnnotationByClassId(LombokNames.SUPER_BUILDER_ID, context.session)?.let { rawAnnotation ->
                    val superBuilder = ConeLombokAnnotations.SuperBuilder.extract(rawAnnotation, context.session)
                    add(superBuilder to superBuilderFlagUsage)
                }
            }
            // `lombok.anyConstructor.flagUsage` covers all three at once, exactly as `lombok.log.flagUsage` covers
            // every log annotation above. Whether the plugin acts on the annotation is a separate question -
            // `@AllArgsConstructor` and `@RequiredArgsConstructor` are `ANNOTATION_IS_NOT_SUPPORTED` on a Kotlin
            // class - because `flagUsage` is about writing the annotation at all, as it is for `@SuperBuilder`.
            //
            // Read off the declaration rather than through `LombokService`, as the `@SuperBuilder` branch above
            // also does: its getters answer `null` here for the annotations the plugin generates nothing from, so
            // going through them would flag `@NoArgsConstructor` alone and leave the other two silent.
            val anyConstructorFlagUsage = lombokService.config.anyConstructorFlagUsage
            for ([companion, specificFlagUsage] in listOf(
                ConeLombokAnnotations.NoArgsConstructor to lombokService.config.noArgsConstructorFlagUsage,
                ConeLombokAnnotations.AllArgsConstructor to lombokService.config.allArgsConstructorFlagUsage,
                ConeLombokAnnotations.RequiredArgsConstructor to lombokService.config.requiredArgsConstructorFlagUsage,
            )) {
                val flagUsage = louderFlagUsage(specificFlagUsage, anyConstructorFlagUsage) ?: continue
                declaration.annotations.getAnnotationByClassId(companion.name, context.session)?.let { rawAnnotation ->
                    add(companion.extract(rawAnnotation, context.session) to flagUsage)
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

    /**
     * The louder of an annotation's own `flagUsage` and the umbrella one covering its whole family, either of which
     * may be unset.
     *
     * Mirrors Lombok's `HandlerUtil.handleFlagUsage`, which reads both keys and takes `error` from either before
     * `warning` from either. It prefers the specific key only between equals, and only to pick the name it prints;
     * that choice is invisible here, the diagnostic naming the annotation rather than the key that flagged it.
     */
    private fun louderFlagUsage(specific: FlagUsageValue?, umbrella: FlagUsageValue?): FlagUsageValue? =
        listOfNotNull(specific, umbrella).maxOrNull()
}
