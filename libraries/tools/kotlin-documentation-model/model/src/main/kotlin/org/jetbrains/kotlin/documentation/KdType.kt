/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A type-argument projection, e.g. `in T`, `out T`, `T`, or `*`. */
@Serializable
sealed class KdTypeProjection {
    @Serializable
    @SerialName("invariant")
    data class Invariant(val type: KdType) : KdTypeProjection()

    @Serializable
    @SerialName("in")
    data class In(val type: KdType) : KdTypeProjection()

    @Serializable
    @SerialName("out")
    data class Out(val type: KdType) : KdTypeProjection()

    @Serializable
    @SerialName("star")
    data object Star : KdTypeProjection()
}

/** The declared variance of a type parameter, e.g. `out T` or `in T`. */
@Serializable
enum class KdTypeVariance { IN, OUT }

/** A single qualifier of a (possibly nested) class-like type, e.g. `A<X>` in `A<X>.B<Y>`. */
@Serializable
data class KdClassTypeQualifier(
    val name: String,
    val typeArguments: List<KdTypeProjection>,
)

/** A base type for all types that can appear in the model, e.g. as a return type or a parameter type. */
@Serializable
sealed class KdType

/** A type referencing a class, interface, object, or other class-like declaration, e.g. `List<String>`. */
@Serializable
@SerialName("classLike")
data class KdClassLikeType(
    val qualifiers: List<KdClassTypeQualifier>,
    val classLikeId: KdElementId,
    val isMarkedNullable: Boolean,
    /** Whether this type is a Java platform type enhanced to non-null by a nullability annotation. */
    val isEnhancedNullability: Boolean = false,
    val annotations: List<KdAnnotation>,
) : KdType()

/** A functional type, e.g. `(A) -> B`. */
@Serializable
@SerialName("functional")
data class KdFunctionalType(
    val returnType: KdTypeProjection,
    val receiverType: KdTypeProjection,
    val valueParameterTypes: List<KdTypeProjection>,
    val contextParameterTypes: List<KdTypeProjection>,
    val isSuspend: Boolean,
    val isMarkedNullable: Boolean,
    val annotations: List<KdAnnotation>,
) : KdType()

/** A reference to a type parameter declared on [containerElementId]. */
@Serializable
@SerialName("typeParameter")
data class KdTypeParameterType(
    val name: String,
    /** The ID of the class or function this type parameter is declared on. */
    val containerElementId: KdElementId,
    val isMarkedNullable: Boolean,
    val annotations: List<KdAnnotation>,
) : KdType()

/** The `dynamic` type (Kotlin/JS only). */
@Serializable
@SerialName("dynamic")
data object KdDynamicType : KdType()

/** A Java flexible (platform) type, e.g. `String!` or `(Mutable)List!`. */
@Serializable
@SerialName("flexible")
data class KdFlexibleType(
    val lowerBound: KdType,
    val upperBound: KdType,
) : KdType()

/** A definitely non-nullable type, e.g. `T & Any`. */
@Serializable
@SerialName("definitelyNotNull")
data class KdDefinitelyNotNullType(
    val original: KdType,
) : KdType()

/** A rich-errors union type: [primaryType] together with the error types it may raise. */
@Serializable
@SerialName("errorUnion")
data class KdErrorUnionType(
    val primaryType: KdType,
    val errorTypes: List<KdType>,
) : KdType()
