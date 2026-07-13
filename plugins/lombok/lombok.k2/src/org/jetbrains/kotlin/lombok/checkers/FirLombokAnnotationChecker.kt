/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

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
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.CACHE_STRATEGY
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.DO_NOT_USE_GETTERS
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.EXCLUDE
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.INCLUDE_RANK
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.OF
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.ON_CONSTRUCTOR
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.ON_PARAM
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.REPLACES
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

object FirLombokAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    private class ImplementedAnnotationsInfo(
        val allowedTargetsMap: Set<KotlinTarget>,
        val unsupportedArguments: Set<Name> = emptySet(),
    )

    private val implementedAnnotationInfos: Map<ClassId, ImplementedAnnotationsInfo> = buildMap {
        val logInfo = ImplementedAnnotationsInfo(
            allowedTargetsMap = setOf(
                KotlinTarget.CLASS_ONLY,
                KotlinTarget.OBJECT,
                KotlinTarget.ENUM_CLASS,
            )
        )
        this[LombokNames.LOG_ID] = logInfo
        this[LombokNames.SLF4J_ID] = logInfo
        this[LombokNames.LOG4J_ID] = logInfo
        this[LombokNames.COMMONS_LOG_ID] = logInfo
        this[LombokNames.FLOGGER_ID] = logInfo
        this[LombokNames.JBOSS_LOG_ID] = logInfo
        this[LombokNames.LOG4J2_ID] = logInfo
        this[LombokNames.XSLF4J_ID] = logInfo
        this[LombokNames.TO_STRING_ID] = ImplementedAnnotationsInfo(
            allowedTargetsMap = setOf(
                KotlinTarget.CLASS_ONLY,
                KotlinTarget.OBJECT,
                KotlinTarget.ENUM_CLASS,
                KotlinTarget.LOCAL_CLASS,
            ),
            unsupportedArguments = setOf(
                EXCLUDE, // Don't support because it will soon be marked as deprecated.
                OF, // Don't support because it will soon be marked as deprecated.
                DO_NOT_USE_GETTERS, // Irrelevant in Kotlin
            )
        )
        this[LombokNames.TO_STRING_INCLUDE_ID] = ImplementedAnnotationsInfo(
            allowedTargetsMap = setOf(
                KotlinTarget.PROPERTY,
                //KotlinTarget.FUNCTION, TODO: support later because Lombok also allows it on functions, KT-86021
            )
        )
        this[LombokNames.TO_STRING_EXCLUDE_ID] = ImplementedAnnotationsInfo(
            allowedTargetsMap = setOf(
                KotlinTarget.PROPERTY,
            )
        )
        this[LombokNames.NO_ARGS_CONSTRUCTOR_ID] = ImplementedAnnotationsInfo(
            allowedTargetsMap = setOf(
                KotlinTarget.CLASS_ONLY, // Objects have empty constructor by default, so doesn't make sense to support the annotation on them.
            ),
            unsupportedArguments = setOf(
                ON_CONSTRUCTOR, // Not yet supported
            )
        )
        this[LombokNames.EQUALS_AND_HASH_CODE_ID] = ImplementedAnnotationsInfo(
            allowedTargetsMap = setOf(
                KotlinTarget.CLASS_ONLY,
                KotlinTarget.OBJECT,
                KotlinTarget.ENUM_CLASS,
                KotlinTarget.LOCAL_CLASS,
            ),
            unsupportedArguments = setOf(
                EXCLUDE, // Don't support because it will soon be marked as deprecated.
                OF, // Don't support because it will soon be marked as deprecated.
                DO_NOT_USE_GETTERS, // Irrelevant in Kotlin
                CACHE_STRATEGY, // Not yet supported
                ON_PARAM, // Not yet supported
            )
        )
        this[LombokNames.EQUALS_AND_HASH_CODE_INCLUDE_ID] = ImplementedAnnotationsInfo(
            allowedTargetsMap = setOf(
                KotlinTarget.PROPERTY,
                //KotlinTarget.FUNCTION, TODO: support later because Lombok also allows it on functions, KT-86021
            ),
            unsupportedArguments = setOf(
                REPLACES, // Not yet supported
                INCLUDE_RANK, // Not yet supported
            )
        )
        this[LombokNames.EQUALS_AND_HASH_CODE_EXCLUDE_ID] = ImplementedAnnotationsInfo(
            allowedTargetsMap = setOf(
                KotlinTarget.PROPERTY,
            )
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        for (annotation in declaration.annotations) {
            val classId = annotation.toAnnotationClassId(context.session) ?: continue
            val implementedAnnotationInfo = implementedAnnotationInfos[classId]

            if (implementedAnnotationInfo != null) {
                val (narrowedAllowedTargets = allowedTargetsMap, unsupportedArguments) = implementedAnnotationInfo
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

                for ([argumentName, argumentExpression] in annotation.argumentMapping.mapping) {
                    if (argumentName == DO_NOT_USE_GETTERS) {
                        reporter.reportOn(
                            argumentExpression.source,
                            LombokFirDiagnostics.DO_NOT_USE_GETTERS_IRRELEVANT,
                            context
                        )
                    } else if (unsupportedArguments.contains(argumentName)) {
                        reporter.reportOn(
                            argumentExpression.source,
                            LombokFirDiagnostics.ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED,
                            argumentName,
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
