/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator.Companion.getPresentableName
import org.jetbrains.dokka.analysis.kotlin.symbols.utils.getLocation
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*

internal const val ERROR_CLASS_NAME = "<ERROR CLASS>"

internal class Location(private val symbol: KaSymbol) {
    fun KaSession.computePath(): String {
        val psi: PsiElement? = symbol.sourcePsiSafe<PsiElement>()
        return if (psi != null)
            getLocation(psi) ?: getDRIFromSymbol(symbol).toString()
        else getDRIFromSymbol(symbol).toString()
    }
}

/**
 * Maps [KaType] to Dokka [Bound] or [TypeConstructorWithKind].
 *
 * Also, build [AncestryNode] tree from [KaType]
 */
internal class TypeTranslator(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val annotationTranslator: AnnotationTranslator,
    private val logger: DokkaLogger
) {

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    private fun KaSession.toProjection(typeProjection: KaTypeProjection, location: Location): Projection =
        when (typeProjection) {
            is KaStarTypeProjection -> Star
            is KaTypeArgumentWithVariance -> toBoundFrom(typeProjection.type, location).wrapWithVariance(typeProjection.variance)
        }

    /**
     * For example,
     * ```
     * typealias Inner = String
     * typealias Outer = Inner
     *
     * val outer: Outer = ""
     * ```
     *
     * `Outer` is [abbreviationType]
     * `String` is [fullyExpandedType]
     */
    private fun KaSession.toBoundFromTypeAliased(abbreviationType: KaClassType, fullyExpandedType: Bound, location: Location): TypeAliased {
        val classSymbol = abbreviationType.symbol
        return if (classSymbol is KaTypeAliasSymbol)
            TypeAliased(
                typeAlias = asNullableIfMarked(
                    abbreviationType,
                    bound = GenericTypeConstructor(
                        dri = getDRIFromClassType(abbreviationType),
                        projections = abbreviationType.typeArguments.map { toProjection(it, location) }
                    )
                ),
                inner = fullyExpandedType,
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(abbreviationType)?.toSourceSetDependent()?.toAnnotations()
                )
            ) else
            throw IllegalStateException("Expected type alias symbol in type")
    }

    private fun KaSession.toTypeConstructorFrom(classType: KaClassType, location: Location) =
        GenericTypeConstructor(
            dri = getDRIFromClassType(classType),
            projections = classType.typeArguments.map { toProjection(it, location) },
            presentableName = classType.getPresentableName(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(classType)?.toSourceSetDependent()?.toAnnotations()
            )
        )

    private fun KaSession.toFunctionalTypeConstructorFrom(functionalType: KaFunctionType, location: Location) = FunctionalTypeConstructor(
        dri = getDRIFromClassType(functionalType),
        projections = functionalType.typeArguments.map { toProjection(it, location) },
        isExtensionFunction = functionalType.receiverType != null,
        isSuspendable = functionalType.isSuspend,
        presentableName = functionalType.getPresentableName(),
        extra = PropertyContainer.withAll(
            getDokkaAnnotationsFrom(functionalType)?.toSourceSetDependent()?.toAnnotations()
        ),
        contextParametersCount = @OptIn(KaExperimentalApi::class) functionalType.contextReceivers.size
    )

    fun KaSession.toBoundFrom(type: KaType, location: Location): Bound {
        val abbreviation = type.abbreviation
        val bound = toBoundFromNoAbbreviation(type, location)
        return when {
            abbreviation != null -> toBoundFromTypeAliased(abbreviation, bound, location)
            else -> bound
        }
    }

    private fun KaSession.toBoundFromNoAbbreviation(type: KaType, location: Location): Bound =
        when (type) {
            is KaUsualClassType -> toTypeConstructorFrom(type, location)
            is KaTypeParameterType -> TypeParameter(
                dri = getDRIFromTypeParameter(type.symbol),
                name = type.name.asString(),
                presentableName = type.getPresentableName(),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )
            is KaErrorType -> {
                logErrorType(type, location)
                @OptIn(KaNonPublicApi::class)
                UnresolvedBound(type.presentableText ?: type.toString())
            }
            is KaFunctionType -> toFunctionalTypeConstructorFrom(type, location)
            is KaDynamicType -> Dynamic
            is KaDefinitelyNotNullType -> DefinitelyNonNullable(
                toBoundFrom(type.original, location)
            )

            is KaFlexibleType -> TypeAliased(
                toBoundFrom(type.upperBound, location),
                toBoundFrom(type.lowerBound, location),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )
            is KaCapturedType -> throw NotImplementedError()
            is KaIntersectionType -> throw NotImplementedError()
            else -> throw NotImplementedError()
        }.let {
            asNullableIfMarked(type, it)
        }

    private fun KaSession.asNullableIfMarked(type: KaType, bound: Bound): Bound =
        if (type.isMarkedNullable) Nullable(bound) else bound


    internal fun KaSession.buildAncestryInformationFrom(
        type: KaType,
        location: Location
    ): AncestryNode {
        val (interfaces, superclass) = type.directSupertypes(true).filterNot { it.isAnyType }
            .partition {
                val typeConstructorWithKind = toTypeConstructorWithKindFrom(it, location)
                typeConstructorWithKind.kind == KotlinClassKindTypes.INTERFACE ||
                        typeConstructorWithKind.kind == JavaClassKindTypes.INTERFACE
            }

        return AncestryNode(
            typeConstructor = toTypeConstructorWithKindFrom(type, location).typeConstructor,
            superclass = superclass.map { buildAncestryInformationFrom(it, location) }.singleOrNull(),
            interfaces = interfaces.map { buildAncestryInformationFrom(it, location) }
        )
    }

    internal fun KaSession.toTypeConstructorWithKindFrom(type: KaType, location: Location): TypeConstructorWithKind = when (type) {
        is KaUsualClassType ->
            when (val classSymbol = type.symbol) {
                is KaNamedClassSymbol -> TypeConstructorWithKind(
                    toTypeConstructorFrom(type, location),
                    classSymbol.classKind.toDokkaClassKind()
                )

                is KaAnonymousObjectSymbol -> throw NotImplementedError()
                is KaTypeAliasSymbol -> toTypeConstructorWithKindFrom(classSymbol.expandedType, location)
            }
        is KaErrorType -> {
            logErrorType(type, location)
            @OptIn(KaNonPublicApi::class)
            TypeConstructorWithKind(
                GenericTypeConstructor(
                    dri = DRI(packageName = "", classNames = type.presentableText ?: "$ERROR_CLASS_NAME $type"),
                    projections = emptyList(),

                    ),
                KotlinClassKindTypes.CLASS
            )
        }

        is KaFunctionType -> TypeConstructorWithKind(
            toFunctionalTypeConstructorFrom(type, location),
            KotlinClassKindTypes.CLASS
        )

        is KaDefinitelyNotNullType -> toTypeConstructorWithKindFrom(type.original, location)

        is KaCapturedType -> throw NotImplementedError()
        is KaDynamicType -> throw NotImplementedError()
        is KaFlexibleType -> throw NotImplementedError()
        is KaIntersectionType -> throw NotImplementedError()
        is KaTypeParameterType -> throw NotImplementedError()
        else -> throw NotImplementedError()
    }

    private fun KaSession.getDokkaAnnotationsFrom(annotated: KaAnnotated): List<Annotations.Annotation>? =
        with(annotationTranslator) { getAllAnnotationsFrom(annotated) }.takeUnless { it.isEmpty() }

    private fun KaClassKind.toDokkaClassKind() = when (this) {
        KaClassKind.CLASS -> KotlinClassKindTypes.CLASS
        KaClassKind.ENUM_CLASS -> KotlinClassKindTypes.ENUM_CLASS
        KaClassKind.ANNOTATION_CLASS -> KotlinClassKindTypes.ANNOTATION_CLASS
        KaClassKind.OBJECT -> KotlinClassKindTypes.OBJECT
        KaClassKind.COMPANION_OBJECT -> KotlinClassKindTypes.OBJECT
        KaClassKind.INTERFACE -> KotlinClassKindTypes.INTERFACE
        KaClassKind.ANONYMOUS_OBJECT -> KotlinClassKindTypes.OBJECT
    }

    private fun KaSession.logErrorType(errorType: KaErrorType, location: Location) {
        @OptIn(KaNonPublicApi::class)
        logger.warn(buildString {
            append(if (errorType.presentableText != null) "`${errorType.presentableText}` is unresolved" else errorType.errorMessage)
            append(" in ")
            append(
                with(location) {
                    computePath()
                })
        }
        )

        @OptIn(KaNonPublicApi::class)
        logger.debug("${errorType.errorMessage}\n" + Thread.currentThread().stackTrace.drop(1).joinToString("\n"))
    }
}
