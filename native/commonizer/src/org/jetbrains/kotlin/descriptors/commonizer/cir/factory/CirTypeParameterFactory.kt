/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirTypeParameterImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.filteredUpperBounds
import org.jetbrains.kotlin.types.Variance

object CirTypeParameterFactory {
    fun create(source: TypeParameterDescriptor): CirTypeParameter = create(
        annotations = source.annotations.compactMap(CirAnnotationFactory::create),
        name = CirName.create(source.name),
        isReified = source.isReified,
        variance = source.variance,
        upperBounds = source.filteredUpperBounds.compactMap(CirTypeFactory::create)
    )

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: CirName,
        isReified: Boolean,
        variance: Variance,
        upperBounds: List<CirType>
    ): CirTypeParameter {
        return CirTypeParameterImpl(
            annotations = annotations,
            name = name,
            isReified = isReified,
            variance = variance,
            upperBounds = upperBounds
        )
    }
}
