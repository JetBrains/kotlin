/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.checkers

import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.lombok.k2.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.ClassId

object FirLombokAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private val implementedKotlinAnnotationsAllowedTargetsMap: Map<ClassId, List<KotlinTarget>> = buildMap {
        val logTargets = listOf(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.OBJECT,
            KotlinTarget.ENUM_CLASS,
        )
        this[LombokNames.LOG_ID] = logTargets
        this[LombokNames.SLF4J_ID] = logTargets
        this[LombokNames.LOG4J_ID] = logTargets
        this[LombokNames.COMMONS_LOG_ID] = logTargets
        this[LombokNames.FLOGGER_ID] = logTargets
        this[LombokNames.JBOSS_LOG_ID] = logTargets
        this[LombokNames.LOG4J2_ID] = logTargets
        this[LombokNames.XSLF4J_ID] = logTargets
        this[LombokNames.TO_STRING_ID] = listOf(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.OBJECT,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.LOCAL_CLASS,
        )
        this[LombokNames.TO_STRING_INCLUDE_ID] = listOf(
            KotlinTarget.PROPERTY,
            //KotlinTarget.FUNCTION, TODO: support later because Lombok also allows it on functions, KT-86021
        )
        this[LombokNames.TO_STRING_EXCLUDE_ID] = listOf(
            KotlinTarget.PROPERTY,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        for (annotation in declaration.annotations) {
            val classId = annotation.toAnnotationClassId(context.session) ?: continue
            val narrowedAllowedTargets = implementedKotlinAnnotationsAllowedTargetsMap[classId]

            if (narrowedAllowedTargets != null) {
                val defaultTargets = getActualTargetList(declaration).defaultTargets

                if (defaultTargets.none { narrowedAllowedTargets.contains(it) }) {
                    val allowedAnnotationTargets = annotation.getAllowedAnnotationTargets(context.session)
                    if (defaultTargets.any { allowedAnnotationTargets.contains(it) }) {
                        reporter.reportOn(
                            annotation.source,
                            LombokFirDiagnostics.ANNOTATION_HAS_NO_EFFECT,
                            defaultTargets.firstOrNull()?.description ?: "unidentified target",
                            narrowedAllowedTargets,
                        )
                    }
                }
            } else if (classId.packageFqName.startsWith(LombokNames.LOMBOK)) {
                reporter.reportOn(
                    annotation.source,
                    LombokFirDiagnostics.ANNOTATION_IS_NOT_SUPPORTED,
                    classId.shortClassName,
                    context,
                )
            }
        }
    }
}
