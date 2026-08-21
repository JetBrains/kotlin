/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.config.CallSuperMode
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.ACCESS
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.CACHE_STRATEGY
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.DO_NOT_USE_GETTERS
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.EXCLUDE
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.INCLUDE_RANK
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.OF
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.ON_CONSTRUCTOR
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.ON_PARAM
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.REPLACES
import org.jetbrains.kotlin.lombok.config.getAccessLevel
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private class ImplementedAnnotationsInfo(
    val allowedTargetsMap: Set<KotlinTarget>,
    val unsupportedArguments: Set<Name> = emptySet(),
    /**
     * A value class is a plain `CLASS` as far as [KotlinTarget] is concerned, so an annotation that cannot act
     * upon one has to say so separately.
     */
    val isSupportedOnValueClass: Boolean = true,
)

private val implementedAnnotationInfos: Map<ClassId, ImplementedAnnotationsInfo> = buildMap {
    val logInfo = ImplementedAnnotationsInfo(
        allowedTargetsMap = setOf(
            KotlinTarget.CLASS_ONLY,
            // `STANDALONE_OBJECT` rather than the umbrella `OBJECT`, which also covers a companion object:
            // `lombok.log.fieldIsStatic` alone decides whether the logger is static, so putting the annotation on
            // the companion object rather than on its class buys nothing - and with `fieldIsStatic=false` it did
            // nothing at all, neither generating nor reporting (KT-88288).
            KotlinTarget.STANDALONE_OBJECT,
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
        ),
        // A value class *is* its underlying value: it has no instance to initialize field by field, and its
        // constructors compile to static `constructor-impl` functions that must return that value. A generated
        // constructor that only calls the superclass one leaves nothing to return, and the JVM backend used to
        // fail outright on its instance initializer with "Unexpected IR element found during code generation"
        // (KT-88705).
        isSupportedOnValueClass = false,
    )
    this[LombokNames.EQUALS_AND_HASH_CODE_ID] = ImplementedAnnotationsInfo(
        allowedTargetsMap = setOf(
            KotlinTarget.CLASS_ONLY,
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
    this[LombokNames.BUILDER_ID] = ImplementedAnnotationsInfo(
        allowedTargetsMap = setOf(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.CONSTRUCTOR,
            KotlinTarget.FUNCTION,
        )
    )
    this[LombokNames.BUILDER_DEFAULT_ID] = ImplementedAnnotationsInfo(
        allowedTargetsMap = setOf(
            KotlinTarget.PROPERTY,
        )
    )
    this[LombokNames.SINGULAR_ID] = ImplementedAnnotationsInfo(
        allowedTargetsMap = setOf(
            KotlinTarget.PROPERTY,
            KotlinTarget.VALUE_PARAMETER,
        )
    )
}

/**
 * Reports the Lombok annotations among [annotations] that the plugin can't act upon, and validates the arguments
 * of the ones it can. Shared by [FirLombokDeclarationAnnotationChecker] and [FirLombokExpressionAnnotationChecker].
 *
 * [defaultTargets] is what the annotated element is, expressed in the terms an annotation's `@Target` speaks:
 * [getActualTargetList] for a declaration, plain [KotlinTarget.EXPRESSION] for an expression.
 *
 * [isValueClass] tells whether the annotated declaration is a value class, which [defaultTargets] cannot express:
 * [KotlinTarget] knows it only as a plain `CLASS`.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkLombokAnnotations(annotations: List<FirAnnotation>, defaultTargets: List<KotlinTarget>, isValueClass: Boolean = false) {
    for (annotation in annotations) {
        val classId = annotation.toAnnotationClassId(context.session) ?: continue
        val implementedAnnotationInfo = implementedAnnotationInfos[classId]

        if (implementedAnnotationInfo != null) {
            val (narrowedAllowedTargets = allowedTargetsMap, unsupportedArguments, isSupportedOnValueClass) = implementedAnnotationInfo

            val ineffectiveTarget = when {
                isValueClass && !isSupportedOnValueClass -> "value class"
                defaultTargets.none { narrowedAllowedTargets.contains(it) } -> {
                    // Only warn where the platform itself accepts the annotation, otherwise
                    // `WRONG_ANNOTATION_TARGET` says it already.
                    val allowedAnnotationTargets = annotation.getAllowedAnnotationTargets(context.session)
                    runIf(defaultTargets.any { allowedAnnotationTargets.contains(it) }) {
                        defaultTargets.firstOrNull()?.description ?: "unidentified target"
                    }
                }
                else -> null
            }

            if (ineffectiveTarget != null) {
                reporter.reportOn(
                    annotation.source,
                    LombokFirDiagnostics.ANNOTATION_HAS_NO_EFFECT,
                    ineffectiveTarget,
                    narrowedAllowedTargets,
                )
            }

            for ([argumentName, argumentExpression] in annotation.argumentMapping.mapping) {
                when (argumentName) {
                    DO_NOT_USE_GETTERS -> {
                        reporter.reportOn(
                            argumentExpression.source,
                            LombokFirDiagnostics.DO_NOT_USE_GETTERS_IRRELEVANT,
                            context
                        )
                    }
                    ACCESS -> {
                        @OptIn(DirectDeclarationsAccess::class)
                        val accessLevel = annotation.getAccessLevel(ACCESS)
                        if (accessLevel == AccessLevel.PACKAGE || accessLevel == AccessLevel.MODULE) {
                            reporter.reportOn(
                                argumentExpression.source,
                                LombokFirDiagnostics.UNSUPPORTED_ACCESS_LEVEL,
                                Name.identifier(accessLevel.name),
                            )
                        }
                    }
                    in unsupportedArguments -> {
                        reporter.reportOn(
                            argumentExpression.source,
                            LombokFirDiagnostics.ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED,
                            argumentName,
                        )
                    }
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

/**
 * Mirrors Lombok behavior: when `*.callSuper=warn` is configured and the
 * annotated class has a non-trivial superclass, warn that the generated function (`toString` or `equals`/`hashCode`) will
 * not chain to it.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkCallSuper(
    callSuperMode: CallSuperMode,
    annotationInfo: ConeLombokAnnotations.ConeLombokAnnotation,
    declaration: FirRegularClass,
    functionNames: Set<Name>,
) {
    if (callSuperMode == CallSuperMode.Warn &&
        declaration.symbol.getSuperClassSymbolOrAny(context.session).let { it != null && it.classId != StandardClassIds.Any }
    ) {
        reporter.reportOn(
            annotationInfo.annotation.source,
            LombokFirDiagnostics.CALL_SUPER_NOT_CALLED,
            functionNames.joinToString("/"),
            annotationInfo.annotation.resolvedType.lookupTagIfAny?.name!!,
            context,
        )
    }
}
