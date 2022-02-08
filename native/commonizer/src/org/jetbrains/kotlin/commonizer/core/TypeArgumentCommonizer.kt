/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirRegularTypeProjection
import org.jetbrains.kotlin.commonizer.cir.CirStarTypeProjection
import org.jetbrains.kotlin.commonizer.cir.CirTypeProjection
import org.jetbrains.kotlin.commonizer.utils.safeCastValues
import org.jetbrains.kotlin.commonizer.utils.singleDistinctValueOrNull

class TypeArgumentCommonizer(
    private val typeCommonizer: TypeCommonizer
) : NullableSingleInvocationCommonizer<CirTypeProjection> {
    override fun invoke(values: List<CirTypeProjection>): CirTypeProjection? {
        /* All values are star projections */
        values.safeCastValues<CirTypeProjection, CirStarTypeProjection>()?.let { return CirStarTypeProjection }

        /* All values are regular type projections */
        val projections = values.safeCastValues<CirTypeProjection, CirRegularTypeProjection>() ?: return null

        return CirRegularTypeProjection(
            projectionKind = projections.singleDistinctValueOrNull { it.projectionKind } ?: return null,
            type = typeCommonizer(projections.map { it.type }) ?: return null
        )
    }
}
