/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getCompilerRequiredAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.config.LombokService
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.Singulars
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkDefaultAndSingular(parameter: FirValueParameterSymbol?, property: FirPropertySymbol?) {
    // The same lookup the generator does: `@Singular` is on the parameter unless it was written with a
    // `@field:` target, and `@Builder.Default` (`@Target(FIELD)`) is only ever on the promoted property.
    val singularAnnotation = parameter?.getCompilerRequiredAnnotationByClassId(LombokNames.SINGULAR_ID)
        ?: property?.backingFieldSymbol?.getCompilerRequiredAnnotationByClassId(LombokNames.SINGULAR_ID)
    val defaultAnnotation = property?.findAnnotationOnPropertyOrField(LombokNames.BUILDER_DEFAULT_ID)

    val lombokService = context.session.lombokService
    if (singularAnnotation != null) {
        checkSingular(parameter ?: property!!, singularAnnotation, lombokService)

        if (defaultAnnotation != null) {
            reporter.reportOn(defaultAnnotation.source, LombokFirDiagnostics.BUILDER_DEFAULT_AND_SINGULAR_MIXED, context)
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkSingular(variable: FirVariableSymbol<*>, singularAnnotation: FirAnnotation, lombokService: LombokService) {
    val singular = ConeLombokAnnotations.Singular.extract(singularAnnotation, context.session)
    val source = singularAnnotation.source

    if (singular.singularName == null) {
        if (!lombokService.config.singularAuto) {
            reporter.reportOn(source, LombokFirDiagnostics.SINGULAR_REQUIRES_EXPLICIT_NAME, context)
        } else if (Singulars.autoSingularize(variable.name.identifier) == null) {
            reporter.reportOn(source, LombokFirDiagnostics.CANNOT_SINGULARIZE_NAME, context)
        }
    }

    val classId = variable.resolvedReturnType.classId
    if (classId != null &&
        classId !in LombokNames.SUPPORTED_COLLECTION_IDS &&
        classId !in LombokNames.SUPPORTED_MAP_IDS &&
        classId !in LombokNames.SUPPORTED_TABLE_IDS
    ) {
        reporter.reportOn(source, LombokFirDiagnostics.UNSUPPORTED_SINGULAR_TYPE, variable.resolvedReturnType, context)
    } else if (classId != null && lombokService.config.singularUseGuava) {
        // Lombok's Guava singularizers build the field with a Guava immutable collection, so without Guava on the
        // classpath javac rejects what Lombok generates, and this mirrors its error.
        val immutableClassId = useGuavaImmutableClassId(classId)
        if (immutableClassId != null && context.session.symbolProvider.getClassLikeSymbolByClassId(immutableClassId) == null) {
            reporter.reportOn(source, LombokFirDiagnostics.SINGULAR_REQUIRES_GUAVA, immutableClassId.asSingleFqName(), context)
        }
    }
}

internal val FirRegularClass.isBuilderCapableClass: Boolean
    get() = symbol.isBuilderCapableClass

internal val FirRegularClassSymbol.isBuilderCapableClass: Boolean
    get() = classKind == ClassKind.CLASS && visibility != Visibilities.Local && !isInner
            && modality != Modality.SEALED && modality != Modality.ABSTRACT

/**
 * The Guava immutable class `lombok.singular.useGuava` builds a `@Singular` field declared with [classId] as, the
 * same mapping as `BuilderBodyBuilder` uses, or `null` for a field declared with a Guava type already.
 */
private fun useGuavaImmutableClassId(classId: ClassId): ClassId? = when (classId) {
    in LombokNames.SUPPORTED_GUAVA_COLLECTION_IDS,
    in LombokNames.SUPPORTED_TABLE_IDS,
    LombokNames.IMMUTABLE_MAP_ID, LombokNames.IMMUTABLE_BI_MAP_ID, LombokNames.IMMUTABLE_SORTED_MAP_ID,
        -> null
    LombokNames.JAVA_SORTED_SET_ID, LombokNames.JAVA_NAVIGABLE_SET_ID -> LombokNames.IMMUTABLE_SORTED_SET_ID
    LombokNames.JAVA_SORTED_MAP_ID, LombokNames.JAVA_NAVIGABLE_MAP_ID -> LombokNames.IMMUTABLE_SORTED_MAP_ID
    in LombokNames.SUPPORTED_MAP_IDS -> LombokNames.IMMUTABLE_MAP_ID
    StandardClassIds.Set, StandardClassIds.MutableSet, LombokNames.JAVA_SET_ID -> LombokNames.IMMUTABLE_SET_ID
    in LombokNames.SUPPORTED_COLLECTION_IDS -> LombokNames.IMMUTABLE_LIST_ID
    else -> null
}
