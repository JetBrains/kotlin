/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeParameterFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.types.Variance

class TypeParameterCommonizer(classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirTypeParameter, CirTypeParameter>() {
    private lateinit var name: CirName
    private var isReified = false
    private lateinit var variance: Variance
    private val upperBounds = TypeParameterUpperBoundsCommonizer(classifiers)

    override fun commonizationResult() = CirTypeParameterFactory.create(
        annotations = emptyList(),
        name = name,
        isReified = isReified,
        variance = variance,
        upperBounds = upperBounds.result
    )

    override fun initialize(first: CirTypeParameter) {
        name = first.name
        isReified = first.isReified
        variance = first.variance
    }

    override fun doCommonizeWith(next: CirTypeParameter) =
        name == next.name
                && isReified == next.isReified
                && variance == next.variance
                && upperBounds.commonizeWith(next.upperBounds)
}

private class TypeParameterUpperBoundsCommonizer(classifiers: CirKnownClassifiers) : AbstractListCommonizer<CirType, CirType>(
    singleElementCommonizerFactory = { TypeCommonizer(classifiers) }
)
