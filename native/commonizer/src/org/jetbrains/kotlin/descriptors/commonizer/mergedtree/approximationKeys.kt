/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeSignature
import org.jetbrains.kotlin.descriptors.commonizer.core.Commonizer
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.descriptors.commonizer.utils.signature
import org.jetbrains.kotlin.name.Name

/** Used for approximation of [PropertyDescriptor]s before running concrete [Commonizer]s */
data class PropertyApproximationKey(
    val name: Name,
    val extensionReceiverParameter: CirTypeSignature?
) {
    constructor(property: PropertyDescriptor) : this(
        property.name.intern(),
        property.extensionReceiverParameter?.type?.signature
    )
}

/** Used for approximation of [SimpleFunctionDescriptor]s before running concrete [Commonizer]s */
data class FunctionApproximationKey(
    val name: Name,
    val valueParameters: List<CirTypeSignature>,
    val extensionReceiverParameter: CirTypeSignature?
) {
    constructor(function: SimpleFunctionDescriptor) : this(
        function.name.intern(),
        function.valueParameters.map { it.type.signature },
        function.extensionReceiverParameter?.type?.signature
    )
}

/** Used for approximation of [ConstructorDescriptor]s before running concrete [Commonizer]s */
data class ConstructorApproximationKey(
    val valueParameters: List<CirTypeSignature>
) {
    constructor(constructor: ConstructorDescriptor) : this(
        constructor.valueParameters.map { it.type.signature }
    )
}