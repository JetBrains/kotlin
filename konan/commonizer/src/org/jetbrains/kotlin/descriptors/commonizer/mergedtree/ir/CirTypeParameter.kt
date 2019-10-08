/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface CirTypeParameter {
    val annotations: Annotations
    val name: Name
    val isReified: Boolean
    val variance: Variance
    val upperBounds: List<CirType>
}

data class CirCommonTypeParameter(
    override val name: Name,
    override val isReified: Boolean,
    override val variance: Variance,
    override val upperBounds: List<CirType>
) : CirTypeParameter {
    override val annotations get() = Annotations.EMPTY
}

data class CirWrappedTypeParameter(private val wrapped: TypeParameterDescriptor) : CirTypeParameter {
    override val annotations get() = wrapped.annotations
    override val name get() = wrapped.name
    override val isReified get() = wrapped.isReified
    override val variance get() = wrapped.variance
    override val upperBounds by lazy(PUBLICATION) { wrapped.upperBounds.map(CirType.Companion::create) }
}
