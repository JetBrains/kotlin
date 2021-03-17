/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.types.Variance

interface CirTypeParameter : CirHasAnnotations, CirHasName {
    val isReified: Boolean
    val variance: Variance
    val upperBounds: List<CirType>

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline fun create(
            annotations: List<CirAnnotation>,
            name: CirName,
            isReified: Boolean,
            variance: Variance,
            upperBounds: List<CirType>
        ): CirTypeParameter = CirTypeParameterImpl(
            annotations = annotations,
            name = name,
            isReified = isReified,
            variance = variance,
            upperBounds = upperBounds
        )
    }
}

data class CirTypeParameterImpl(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    override val isReified: Boolean,
    override val variance: Variance,
    override val upperBounds: List<CirType>
) : CirTypeParameter
