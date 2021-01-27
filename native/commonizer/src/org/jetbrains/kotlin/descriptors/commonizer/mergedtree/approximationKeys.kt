/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeSignature
import org.jetbrains.kotlin.descriptors.commonizer.core.Commonizer
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.descriptors.commonizer.utils.signature
import org.jetbrains.kotlin.name.Name

/** Used for approximation of [PropertyDescriptor]s before running concrete [Commonizer]s */
data class PropertyApproximationKey(
    val name: Name,
    val extensionReceiverParameterType: CirTypeSignature?
) {
    constructor(property: PropertyDescriptor) : this(
        property.name.intern(),
        property.extensionReceiverParameter?.type?.signature
    )
}

/** Used for approximation of [SimpleFunctionDescriptor]s before running concrete [Commonizer]s */
data class FunctionApproximationKey(
    val name: Name,
    val valueParametersTypes: Array<CirTypeSignature>,
    private val additionalValueParametersNamesHash: Int,
    val extensionReceiverParameterType: CirTypeSignature?
) {
    constructor(function: SimpleFunctionDescriptor) : this(
        function.name.intern(),
        function.valueParameters.toTypeSignatures(),
        additionalValueParameterNamesHash(function),
        function.extensionReceiverParameter?.type?.signature
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

/** Used for approximation of [ConstructorDescriptor]s before running concrete [Commonizer]s */
data class ConstructorApproximationKey(
    val valueParametersTypes: Array<CirTypeSignature>,
    private val additionalValueParametersNamesHash: Int
) {
    constructor(constructor: ConstructorDescriptor) : this(
        constructor.valueParameters.toTypeSignatures(),
        additionalValueParameterNamesHash(constructor)
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
private inline fun List<ValueParameterDescriptor>.toTypeSignatures(): Array<CirTypeSignature> =
    Array(size) { index -> this[index].type.signature }

private fun additionalValueParameterNamesHash(callable: FunctionDescriptor): Int {
    // TODO: add more precise checks when more languages than C & ObjC are supported
    if (callable.annotations.none { it.isObjCInteropCallableAnnotation })
        return 0 // do not calculate hash for non-ObjC callables

    return callable.valueParameters.fold(0) { acc, next -> acc.appendHashCode(next.name) }
}
