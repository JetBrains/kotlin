/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeSignature
import org.jetbrains.kotlin.descriptors.commonizer.core.Commonizer
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.descriptors.commonizer.utils.signature
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.name.Name

/** Used for approximation of [PropertyDescriptor]s before running concrete [Commonizer]s */
data class PropertyApproximationKey(
    private val name: Name,
    private val extensionReceiverParameterType: CirTypeSignature?
) {
    constructor(property: PropertyDescriptor) : this(
        property.name.intern(),
        property.extensionReceiverParameter?.type?.signature
    )
}

/** Used for approximation of [SimpleFunctionDescriptor]s before running concrete [Commonizer]s */
class FunctionApproximationKey(
    private val name: Name,
    private val valueParametersTypes: Array<CirTypeSignature>,
    private val extensionReceiverParameterType: CirTypeSignature?
) {
    constructor(function: SimpleFunctionDescriptor) : this(
        function.name.intern(),
        function.valueParameters.toTypeSignatures(),
        function.extensionReceiverParameter?.type?.signature
    )

    override fun equals(other: Any?): Boolean {
        if (other !is FunctionApproximationKey)
            return false

        return name == other.name
                && valueParametersTypes.contentEquals(other.valueParametersTypes)
                && extensionReceiverParameterType == other.extensionReceiverParameterType
    }

    override fun hashCode() = hashCode(name)
        .appendHashCode(valueParametersTypes)
        .appendHashCode(extensionReceiverParameterType)
}

/** Used for approximation of [ConstructorDescriptor]s before running concrete [Commonizer]s */
class ConstructorApproximationKey(
    private val valueParametersTypes: Array<CirTypeSignature>
) {
    constructor(constructor: ConstructorDescriptor) : this(
        constructor.valueParameters.toTypeSignatures()
    )

    override fun equals(other: Any?): Boolean {
        if (other !is ConstructorApproximationKey)
            return false

        return valueParametersTypes.contentEquals(other.valueParametersTypes)
    }

    override fun hashCode() = hashCode(valueParametersTypes)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun List<ValueParameterDescriptor>.toTypeSignatures(): Array<CirTypeSignature> =
    Array(size) { index -> this[index].type.signature }
