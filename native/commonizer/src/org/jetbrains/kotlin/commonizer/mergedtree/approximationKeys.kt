/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import kotlinx.metadata.*
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirTypeSignature
import org.jetbrains.kotlin.commonizer.core.Commonizer
import org.jetbrains.kotlin.commonizer.metadata.CirTypeParameterResolver
import org.jetbrains.kotlin.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.commonizer.utils.computeSignature
import org.jetbrains.kotlin.commonizer.utils.hashCode
import org.jetbrains.kotlin.commonizer.utils.isObjCInteropCallableAnnotation

/** Used for approximation of [KmProperty]s before running concrete [Commonizer]s */
data class PropertyApproximationKey(
    val name: CirName,
    val extensionReceiverParameterType: CirTypeSignature?
) {
    constructor(property: KmProperty, typeParameterResolver: CirTypeParameterResolver) : this(
        CirName.create(property.name),
        property.receiverParameterType?.computeSignature(typeParameterResolver)
    )
}

/** Used for approximation of [KmFunction]s before running concrete [Commonizer]s */
data class FunctionApproximationKey(
    val name: CirName,
    val valueParametersTypes: Array<CirTypeSignature>,
    private val additionalValueParametersNamesHash: Int,
    val extensionReceiverParameterType: CirTypeSignature?
) {
    constructor(function: KmFunction, typeParameterResolver: CirTypeParameterResolver) : this(
        CirName.create(function.name),
        function.valueParameters.computeSignatures(typeParameterResolver),
        additionalValueParameterNamesHash(function.annotations, function.valueParameters),
        function.receiverParameterType?.computeSignature(typeParameterResolver)
    )

    override fun equals(other: Any?): Boolean {
        if (other !is FunctionApproximationKey)
            return false

        return name == other.name
                && additionalValueParametersNamesHash == other.additionalValueParametersNamesHash
                && valueParametersTypes.contentEquals(other.valueParametersTypes)
                && extensionReceiverParameterType == other.extensionReceiverParameterType
    }

    override fun hashCode() = hashCode(name)
        .appendHashCode(valueParametersTypes)
        .appendHashCode(extensionReceiverParameterType)
        .appendHashCode(additionalValueParametersNamesHash)
}

/** Used for approximation of [KmConstructor]s before running concrete [Commonizer]s */
data class ConstructorApproximationKey(
    val valueParametersTypes: Array<CirTypeSignature>,
    private val additionalValueParametersNamesHash: Int
) {
    constructor(constructor: KmConstructor, typeParameterResolver: CirTypeParameterResolver) : this(
        constructor.valueParameters.computeSignatures(typeParameterResolver),
        additionalValueParameterNamesHash(constructor.annotations, constructor.valueParameters)
    )

    override fun equals(other: Any?): Boolean {
        if (other !is ConstructorApproximationKey)
            return false

        return additionalValueParametersNamesHash == other.additionalValueParametersNamesHash
                && valueParametersTypes.contentEquals(other.valueParametersTypes)
    }

    override fun hashCode() = hashCode(valueParametersTypes)
        .appendHashCode(additionalValueParametersNamesHash)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun List<KmValueParameter>.computeSignatures(typeParameterResolver: CirTypeParameterResolver): Array<CirTypeSignature> =
    if (isEmpty()) emptyArray() else Array(size) { index -> this[index].type?.computeSignature(typeParameterResolver).orEmpty() }

private fun additionalValueParameterNamesHash(annotations: List<KmAnnotation>, valueParameters: List<KmValueParameter>): Int {
    // TODO: add more precise checks when more languages than C & ObjC are supported
    if (annotations.none { it.isObjCInteropCallableAnnotation })
        return 0 // do not calculate hash for non-ObjC callables

    return valueParameters.fold(0) { acc, next -> acc.appendHashCode(next.name) }
}
