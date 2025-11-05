/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator.Companion.getPresentableName
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*

internal const val ERROR_CLASS_NAME = "<ERROR CLASS>"

/**
 * Maps [KaType] to Dokka [Bound] or [TypeConstructorWithKind].
 *
 * Also, build [AncestryNode] tree from [KaType]
 */
internal class TypeTranslator(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val annotationTranslator: AnnotationTranslator,
) {

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    private fun KaSession.toProjection(typeProjection: KaTypeProjection): Projection =
        when (typeProjection) {
            is KaStarTypeProjection -> Star
            is KaTypeArgumentWithVariance -> toBoundFrom(typeProjection.type).wrapWithVariance(typeProjection.variance)
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
    private fun KaSession.toBoundFromTypeAliased(abbreviationType: KaClassType, fullyExpandedType: Bound): TypeAliased {
        val classSymbol = abbreviationType.symbol
        return if (classSymbol is KaTypeAliasSymbol)
            TypeAliased(
                typeAlias = asNullableIfMarked(
                    abbreviationType,
                    bound = GenericTypeConstructor(
                        dri = getDRIFromClassType(abbreviationType),
                        projections = abbreviationType.typeArguments.map { toProjection(it) }
                    )
                ),
                inner = fullyExpandedType,
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(abbreviationType)?.toSourceSetDependent()?.toAnnotations()
                )
            ) else
            throw IllegalStateException("Expected type alias symbol in type")
    }

    private fun KaSession.toTypeConstructorFrom(classType: KaClassType) =
        GenericTypeConstructor(
            dri = getDRIFromClassType(classType),
            projections = classType.typeArguments.map { toProjection(it) },
            presentableName = classType.getPresentableName(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(classType)?.toSourceSetDependent()?.toAnnotations()
            )
        )

    private fun KaSession.toFunctionalTypeConstructorFrom(functionalType: KaFunctionType) = FunctionalTypeConstructor(
        dri = getDRIFromClassType(functionalType),
        projections = functionalType.typeArguments.map { toProjection(it) },
        isExtensionFunction = functionalType.receiverType != null,
        isSuspendable = functionalType.isSuspend,
        presentableName = functionalType.getPresentableName(),
        extra = PropertyContainer.withAll(
            getDokkaAnnotationsFrom(functionalType)?.toSourceSetDependent()?.toAnnotations()
        ),
        contextParametersCount = @OptIn(KaExperimentalApi::class) functionalType.contextReceivers.size
    )

    fun KaSession.toBoundFrom(type: KaType): Bound {
        val abbreviation = type.abbreviation
        val bound = toBoundFromNoAbbreviation(type)
        return when {
            abbreviation != null -> toBoundFromTypeAliased(abbreviation, bound)
            else -> bound
        }
    }

    fun KaSession.toBoundFromNoAbbreviation(type: KaType): Bound =
        when (type) {
            is KaUsualClassType -> toTypeConstructorFrom(type)
            is KaTypeParameterType -> TypeParameter(
                dri = getDRIFromTypeParameter(type.symbol),
                name = type.name.asString(),
                presentableName = type.getPresentableName(),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )

            is KaClassErrorType -> UnresolvedBound(type.toString())
            is KaFunctionType -> toFunctionalTypeConstructorFrom(type)
            is KaDynamicType -> Dynamic
            is KaDefinitelyNotNullType -> DefinitelyNonNullable(
                toBoundFrom(type.original)
            )

            is KaFlexibleType -> TypeAliased(
                toBoundFrom(type.upperBound),
                toBoundFrom(type.lowerBound),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )

            is KaErrorType -> UnresolvedBound(type.toString())
            is KaCapturedType -> throw NotImplementedError()
            is KaIntersectionType -> throw NotImplementedError()
            else -> throw NotImplementedError()
        }.let {
            asNullableIfMarked(type, it)
        }

    private fun KaSession.asNullableIfMarked(type: KaType, bound: Bound): Bound =
        if (type.isMarkedNullable) Nullable(bound) else bound


    fun KaSession.buildAncestryInformationFrom(
        type: KaType
    ): AncestryNode {
        val (interfaces, superclass) = type.directSupertypes(true).filterNot { it.isAnyType }
            .partition {
                val typeConstructorWithKind = toTypeConstructorWithKindFrom(it)
                typeConstructorWithKind.kind == KotlinClassKindTypes.INTERFACE ||
                        typeConstructorWithKind.kind == JavaClassKindTypes.INTERFACE
            }

        return AncestryNode(
            typeConstructor = toTypeConstructorWithKindFrom(type).typeConstructor,
            superclass = superclass.map { buildAncestryInformationFrom(it) }.singleOrNull(),
            interfaces = interfaces.map { buildAncestryInformationFrom(it) }
        )
    }

    internal fun KaSession.toTypeConstructorWithKindFrom(type: KaType): TypeConstructorWithKind = when (type) {
        is KaUsualClassType ->
            when (val classSymbol = type.symbol) {
                is KaNamedClassSymbol -> TypeConstructorWithKind(
                    toTypeConstructorFrom(type),
                    classSymbol.classKind.toDokkaClassKind()
                )

                is KaAnonymousObjectSymbol -> throw NotImplementedError()
                is KaTypeAliasSymbol -> toTypeConstructorWithKindFrom(classSymbol.expandedType)
            }

        is KaClassErrorType -> TypeConstructorWithKind(
            GenericTypeConstructor(
                dri = DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type"),
                projections = emptyList(),

                ),
            KotlinClassKindTypes.CLASS
        )

        is KaErrorType -> TypeConstructorWithKind(
            GenericTypeConstructor(
                dri = DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type"),
                projections = emptyList(),

                ),
            KotlinClassKindTypes.CLASS
        )

        is KaFunctionType -> TypeConstructorWithKind(
            toFunctionalTypeConstructorFrom(type),
            KotlinClassKindTypes.CLASS
        )

        is KaDefinitelyNotNullType -> toTypeConstructorWithKindFrom(type.original)

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
}
