/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

interface CirTypeParameter {
    val annotations: List<CirAnnotation>
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
    override val annotations: List<CirAnnotation> get() = emptyList()
}

class CirTypeParameterImpl(original: TypeParameterDescriptor) : CirTypeParameter {
    override val annotations = original.annotations.map(CirAnnotation.Companion::create)
    override val name = original.name.intern()
    override val isReified = original.isReified
    override val variance = original.variance
    override val upperBounds = original.upperBounds.map(CirType.Companion::create)
}
