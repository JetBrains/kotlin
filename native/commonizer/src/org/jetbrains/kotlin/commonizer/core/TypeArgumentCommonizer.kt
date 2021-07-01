/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirRegularTypeProjection
import org.jetbrains.kotlin.commonizer.cir.CirStarTypeProjection
import org.jetbrains.kotlin.commonizer.cir.CirTypeProjection
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.types.Variance

class TypeArgumentCommonizer(
    classifiers: CirKnownClassifiers
) : AbstractStandardCommonizer<CirTypeProjection, CirTypeProjection>() {
    private var isStar = false
    private lateinit var projectionKind: Variance
    private val type = TypeCommonizer(classifiers).asCommonizer()

    override fun commonizationResult() = if (isStar) CirStarTypeProjection else CirRegularTypeProjection(
        projectionKind = projectionKind,
        type = type.result
    )

    override fun initialize(first: CirTypeProjection) {
        when (first) {
            is CirStarTypeProjection -> isStar = true
            is CirRegularTypeProjection -> projectionKind = first.projectionKind
        }
    }

    override fun doCommonizeWith(next: CirTypeProjection) = when (next) {
        is CirStarTypeProjection -> isStar
        is CirRegularTypeProjection -> !isStar && projectionKind == next.projectionKind && type.commonizeWith(next.type)
    }
}
