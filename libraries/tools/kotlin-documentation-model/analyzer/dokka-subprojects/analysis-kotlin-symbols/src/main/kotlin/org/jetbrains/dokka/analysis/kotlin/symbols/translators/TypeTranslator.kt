/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator.Companion.getPresentableName
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*

internal const val ERROR_CLASS_NAME = "<ERROR CLASS>"

/**
 * Maps [KtType] to Dokka [Bound] or [TypeConstructorWithKind].
 *
 * Also, build [AncestryNode] tree from [KtType]
 */
internal class TypeTranslator(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val annotationTranslator: AnnotationTranslator
) {

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    private fun KtAnalysisSession.toProjection(typeProjection: KtTypeProjection): Projection =
        when (typeProjection) {
            is KtStarTypeProjection -> Star
            is KtTypeArgumentWithVariance -> toBoundFrom(typeProjection.type).wrapWithVariance(typeProjection.variance)
        }

    private fun KtAnalysisSession.toBoundFromTypeAliased(classType: KtNonErrorClassType): TypeAliased {
        val classSymbol = classType.classSymbol
        return if (classSymbol is KtTypeAliasSymbol)
            TypeAliased(
                typeAlias = GenericTypeConstructor(
                    dri = getDRIFromNonErrorClassType(classType),
                    projections = classType.ownTypeArguments.map { toProjection(it) }),
                inner = toBoundFrom(classType.fullyExpandedType),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(classType)?.toSourceSetDependent()?.toAnnotations()
                )
            ) else
            throw IllegalStateException("Expected type alias symbol in type")
    }

    private fun KtAnalysisSession.toTypeConstructorFrom(classType: KtNonErrorClassType) =
        GenericTypeConstructor(
            dri = getDRIFromNonErrorClassType(classType),
            projections = classType.ownTypeArguments.map { toProjection(it) },
            presentableName = classType.getPresentableName(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(classType)?.toSourceSetDependent()?.toAnnotations()
            )
        )

    private fun KtAnalysisSession.toFunctionalTypeConstructorFrom(functionalType: KtFunctionalType) =
        FunctionalTypeConstructor(
            dri = getDRIFromNonErrorClassType(functionalType),
            projections = functionalType.ownTypeArguments.map { toProjection(it) },
            isExtensionFunction = functionalType.receiverType != null,
            isSuspendable = functionalType.isSuspend,
            presentableName = functionalType.getPresentableName(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(functionalType)?.toSourceSetDependent()?.toAnnotations()
            )
        )

    fun KtAnalysisSession.toBoundFrom(type: KtType): Bound =
        when (type) {
            is KtUsualClassType -> {
                if (type.classSymbol is KtTypeAliasSymbol) toBoundFromTypeAliased(type)
                else toTypeConstructorFrom(type)
            }

            is KtTypeParameterType -> TypeParameter(
                dri = getDRIFromTypeParameter(type.symbol),
                name = type.name.asString(),
                presentableName = type.getPresentableName(),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )

            is KtClassErrorType -> UnresolvedBound(type.toString())
            is KtFunctionalType -> {
                if (type.classSymbol is KtTypeAliasSymbol) toBoundFromTypeAliased(type)
                else toFunctionalTypeConstructorFrom(type)
            }
            is KtDynamicType -> Dynamic
            is KtDefinitelyNotNullType -> DefinitelyNonNullable(
                toBoundFrom(type.original)
            )

            is KtFlexibleType -> TypeAliased(
                toBoundFrom(type.upperBound),
                toBoundFrom(type.lowerBound),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )

            is KtTypeErrorType -> UnresolvedBound(type.toString())
            is KtCapturedType -> throw NotImplementedError()
            is KtIntegerLiteralType -> throw NotImplementedError()
            is KtIntersectionType -> throw NotImplementedError()
        }.let {
            if (type.isMarkedNullable) Nullable(it) else it
        }

    fun KtAnalysisSession.buildAncestryInformationFrom(
        type: KtType
    ): AncestryNode {
        val (interfaces, superclass) = type.getDirectSuperTypes().filterNot { it.isAny }
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

    internal fun KtAnalysisSession.toTypeConstructorWithKindFrom(type: KtType): TypeConstructorWithKind = when (type) {
        is KtUsualClassType ->
            when (val classSymbol = type.classSymbol) {
                is KtNamedClassOrObjectSymbol -> TypeConstructorWithKind(
                    toTypeConstructorFrom(type),
                    classSymbol.classKind.toDokkaClassKind()
                )

                is KtAnonymousObjectSymbol -> throw NotImplementedError()
                is KtTypeAliasSymbol -> toTypeConstructorWithKindFrom(classSymbol.expandedType)
            }

        is KtClassErrorType -> TypeConstructorWithKind(
            GenericTypeConstructor(
                dri = DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type"),
                projections = emptyList(),

                ),
            KotlinClassKindTypes.CLASS
        )

        is KtTypeErrorType -> TypeConstructorWithKind(
            GenericTypeConstructor(
                dri = DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type"),
                projections = emptyList(),

                ),
            KotlinClassKindTypes.CLASS
        )

        is KtFunctionalType -> TypeConstructorWithKind(
            toFunctionalTypeConstructorFrom(type),
            KotlinClassKindTypes.CLASS
        )

        is KtDefinitelyNotNullType -> toTypeConstructorWithKindFrom(type.original)

        is KtCapturedType -> throw NotImplementedError()
        is KtDynamicType -> throw NotImplementedError()
        is KtFlexibleType -> throw NotImplementedError()
        is KtIntegerLiteralType -> throw NotImplementedError()
        is KtIntersectionType -> throw NotImplementedError()
        is KtTypeParameterType -> throw NotImplementedError()
    }

    private fun KtAnalysisSession.getDokkaAnnotationsFrom(annotated: KtAnnotated): List<Annotations.Annotation>? =
        with(annotationTranslator) { getAllAnnotationsFrom(annotated) }.takeUnless { it.isEmpty() }

    private fun KtClassKind.toDokkaClassKind() = when (this) {
        KtClassKind.CLASS -> KotlinClassKindTypes.CLASS
        KtClassKind.ENUM_CLASS -> KotlinClassKindTypes.ENUM_CLASS
        KtClassKind.ANNOTATION_CLASS -> KotlinClassKindTypes.ANNOTATION_CLASS
        KtClassKind.OBJECT -> KotlinClassKindTypes.OBJECT
        KtClassKind.COMPANION_OBJECT -> KotlinClassKindTypes.OBJECT
        KtClassKind.INTERFACE -> KotlinClassKindTypes.INTERFACE
        KtClassKind.ANONYMOUS_OBJECT -> KotlinClassKindTypes.OBJECT
    }
}
