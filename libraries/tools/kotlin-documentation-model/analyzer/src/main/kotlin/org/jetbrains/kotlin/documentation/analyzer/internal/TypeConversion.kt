/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.kotlin.documentation.*
import org.jetbrains.kotlin.documentation.KdAnnotationArgumentValue.*

// region Annotations

/** Annotations themselves annotated `@MustBeDocumented`/`@Documented`, as computed by Dokka's translators. */
internal fun List<Annotations.Annotation>.toKdAnnotations(): List<KdAnnotation> =
    filter { it.mustBeDocumented }.map { it.toKdAnnotation() }

private fun Annotations.Annotation.toKdAnnotation(): KdAnnotation =
    KdAnnotation(
        classLikeId = dri.toKdElementId(),
        arguments = params.mapValues { it.value.toKdAnnotationArgumentValue() },
    )

//
private fun AnnotationParameterValue.toKdAnnotationArgumentValue(): KdAnnotationArgumentValue = when (this) {
    is AnnotationValue -> Annotation(annotation.toKdAnnotation())
    is ArrayValue -> Array(value.map { it.toKdAnnotationArgumentValue() })
    is EnumValue -> EnumEntry(enumDri.toKdElementId())
    is ClassValue -> Classlike(classDRI.toKdElementId())
    is IntValue -> Const(KdConstExpression(KdConstValue.Int(value), expression = null))
    is LongValue -> Const(KdConstExpression(KdConstValue.Long(value), expression = null))
    is FloatValue -> Const(KdConstExpression(KdConstValue.Float(value), expression = null))
    is DoubleValue -> Const(KdConstExpression(KdConstValue.Double(value), expression = null))
    is BooleanValue -> Const(KdConstExpression(KdConstValue.Boolean(value), expression = null))
    is StringValue -> Const(KdConstExpression(KdConstValue.String(value), expression = null))
    is NullValue -> Const(KdConstExpression(Null, expression = null))
    // TODO new annotation const values: [UInt], [Chat], [Byte] and so on
    is LiteralValue -> Const(KdConstExpression(Null, expression = KdExpression(text(), emptyList())))
}

/**
 * The `@MustBeDocumented`-filtered, directly-applied annotations of the subject of [this] property container, in
 * [sourceSet]. `Annotations`'s key is contravariant in its subject type, so this works for any `C` that's itself an
 * [AnnotationTarget] (type uses, type parameters, declarations, ...).
 */
internal fun <C : AnnotationTarget> PropertyContainer<C>.annotationsIn(sourceSet: DokkaSourceSet): List<KdAnnotation> {
    val annotations = this.get<Annotations>(Annotations) ?: return emptyList()
    return annotations.directAnnotations[sourceSet].orEmpty().toKdAnnotations()
}

// endregion

// region Types

// TODO [FlexibleType] is not present in Documentable model
internal fun Bound.toKdType(sourceSet: DokkaSourceSet): KdType = when (this) {
    is Nullable -> inner.toKdType(sourceSet).withNullable(true)
    is DefinitelyNonNullable -> KdDefinitelyNotNullType(inner.toKdType(sourceSet))
    is TypeParameter -> KdTypeParameterType(
        name = name,
        containerElementId = dri.copy(target = PointingToDeclaration).toKdElementId(),
        isMarkedNullable = false,
        annotations = extra.annotationsIn(sourceSet),
    )
    is GenericTypeConstructor -> toClassLikeType(dri, projections, extra.annotationsIn(sourceSet), sourceSet)
    is FunctionalTypeConstructor -> toFunctionalType(sourceSet)
    is PrimitiveJavaType -> KdClassLikeType(
        qualifiers = listOf(KdClassTypeQualifier(name, emptyList())),
        classLikeId = primitiveJavaTypeKotlinId(name),
        isMarkedNullable = false,
        annotations = extra.annotationsIn(sourceSet),
    )
    is JavaObject -> KdClassLikeType(
        qualifiers = listOf(KdClassTypeQualifier("Object", emptyList())),
        classLikeId = DRI("java.lang", "Object").toKdElementId(),
        isMarkedNullable = false,
        annotations = extra.annotationsIn(sourceSet),
    ).let { lowerBound ->
        KdFlexibleType(lowerBound, lowerBound.copy(isMarkedNullable = true))
    }
    // TODO KT-90413
    is UnresolvedBound -> KdClassLikeType(
        qualifiers = listOf(KdClassTypeQualifier(name, emptyList())),
        classLikeId = DRI(packageName = null, classNames = name).toKdElementId(),
        isMarkedNullable = false,
        annotations = extra.annotationsIn(sourceSet),
    )
    is TypeAliased -> typeAlias.toKdType(sourceSet)
    is Dynamic -> KdDynamicType
    // Java's `void` has no direct KDM equivalent; `Unit` is the closest match for a return type.
    is Void -> KdClassLikeType(
        qualifiers = listOf(KdClassTypeQualifier("Unit", emptyList())),
        classLikeId = DRI("kotlin", "Unit").toKdElementId(),
        isMarkedNullable = false,
        annotations = emptyList(),
    )
}

@OptIn(org.jetbrains.dokka.ExperimentalDokkaApi::class)
private fun FunctionalTypeConstructor.toFunctionalType(sourceSet: DokkaSourceSet): KdFunctionalType {
    val arguments = projections.map { it.toKdTypeProjection(sourceSet) }
    val receiverIndex = contextParametersCount
    val valueParametersStart = receiverIndex + if (isExtensionFunction) 1 else 0
    return KdFunctionalType(
        returnType = arguments.last(),
        receiverType = if (isExtensionFunction) arguments[receiverIndex] else null,
        valueParameterTypes = arguments.subList(valueParametersStart, arguments.lastIndex),
        contextParameterTypes = arguments.take(contextParametersCount),
        isSuspend = isSuspendable,
        isMarkedNullable = false,
        annotations = extra.annotationsIn(sourceSet),
    )
}

private fun toClassLikeType(
    dri: DRI,
    projections: List<Projection>,
    annotations: List<KdAnnotation>,
    sourceSet: DokkaSourceSet,
): KdClassLikeType {
    val simpleName = dri.classNames?.substringAfterLast('.') ?: dri.classNames.orEmpty()
    return KdClassLikeType(
        qualifiers = listOf(KdClassTypeQualifier(simpleName, projections.map { it.toKdTypeProjection(sourceSet) })),
        classLikeId = dri.toKdElementId(),
        isMarkedNullable = false,
        annotations = annotations,
    )
}

private fun Projection.toKdTypeProjection(sourceSet: DokkaSourceSet): KdTypeProjection = when (this) {
    is Invariance<*> -> KdTypeProjection.Invariant(inner.toKdType(sourceSet))
    is Covariance<*> -> KdTypeProjection.Out(inner.toKdType(sourceSet))
    is Contravariance<*> -> KdTypeProjection.In(inner.toKdType(sourceSet))
    Star -> KdTypeProjection.Star
    else -> error("Unknown projection: $this")
}

private fun KdType.withNullable(nullable: Boolean): KdType = when (this) {
    is KdClassLikeType -> copy(isMarkedNullable = nullable)
    is KdFunctionalType -> copy(isMarkedNullable = nullable)
    is KdTypeParameterType -> copy(isMarkedNullable = nullable)
    // The rest of the hierarchy has no `isMarkedNullable` slot; left as-is.
    is KdDefinitelyNotNullType -> this
    is KdDynamicType -> this
    is KdErrorUnionType -> this
    is KdFlexibleType -> this
}

private fun primitiveJavaTypeKotlinId(javaPrimitiveName: String): KdElementId {
    val kotlinName = when (javaPrimitiveName) {
        "int" -> "Int"
        "boolean" -> "Boolean"
        "long" -> "Long"
        "short" -> "Short"
        "byte" -> "Byte"
        "char" -> "Char"
        "float" -> "Float"
        "double" -> "Double"
        "void" -> "Unit"
        else -> javaPrimitiveName.replaceFirstChar { it.uppercase() }
    }
    return DRI("kotlin", kotlinName).toKdElementId()
}

// endregion
