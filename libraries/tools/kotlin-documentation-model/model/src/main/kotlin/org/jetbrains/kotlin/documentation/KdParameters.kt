/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A type parameter declared on a class, function, property, or type alias.
 *
 * Not a separate [KdElement]: it's embedded directly in the declaration it's part of, as neither
 * its name nor its index in the parameter list is stable enough to be referenced from outside of
 * that declaration.
 */
@Serializable
data class KdTypeParameter(
    val name: String,
    val isReified: Boolean,
    val upperBounds: List<KdType>,
    val variance: KdTypeVariance?,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
)

/**
 * An extension receiver parameter of a function or a property; also represents the pseudo-receiver
 * of a companion object's extensions. Not a separate [KdElement], see [KdTypeParameter].
 */
@Serializable
data class KdExtensionReceiverParameter(
    val type: KdType,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
)

/** A context parameter of a function or a property. Not a separate [KdElement], see [KdTypeParameter]. */
@Serializable
data class KdContextParameter(
    val name: String?,
    val type: KdType,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
)

/** A regular value parameter of a function or a constructor. Not a separate [KdElement], see [KdTypeParameter]. */
@Serializable
data class KdValueParameter(
    val name: String,
    val type: KdType,
    val isNoinline: Boolean,
    val isCrossinline: Boolean,
    val isVararg: Boolean,
    val defaultValueExpression: KdDefaultValueExpression?,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation = KdDocumentation.EMPTY,
)

/**
 * The default value of a [KdValueParameter]: either [Defined] with an explicit [KdExpression], or
 * [Inherited] from an overridden/actual declaration whose default value may not be available
 * during analysis (e.g. it comes from another module).
 */
@Serializable
sealed class KdDefaultValueExpression {
    @Serializable
    @SerialName("defined")
    data class Defined(val expression: KdExpression) : KdDefaultValueExpression()

    @Serializable
    @SerialName("inherited")
    data class Inherited(val elementId: KdElementId) : KdDefaultValueExpression()
}
