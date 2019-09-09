/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonTypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.TypeParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance

interface TypeParameterCommonizer : Commonizer<TypeParameter, TypeParameter> {
    companion object {
        fun default(cache: ClassifiersCache): TypeParameterCommonizer = DefaultTypeParameterCommonizer(cache)
    }
}

private class DefaultTypeParameterCommonizer(cache: ClassifiersCache) :
    TypeParameterCommonizer,
    AbstractStandardCommonizer<TypeParameter, TypeParameter>() {

    private lateinit var name: Name
    private var isReified = false
    private lateinit var variance: Variance
    private val upperBounds = TypeParameterUpperBoundsCommonizer(cache)

    override fun commonizationResult() = CommonTypeParameter(
        name = name,
        isReified = isReified,
        variance = variance,
        upperBounds = upperBounds.result
    )

    override fun initialize(first: TypeParameter) {
        name = first.name
        isReified = first.isReified
        variance = first.variance
    }

    override fun doCommonizeWith(next: TypeParameter) =
        name == next.name
                && isReified == next.isReified
                && variance == next.variance
                && upperBounds.commonizeWith(next.upperBounds)
}

private class TypeParameterUpperBoundsCommonizer(cache: ClassifiersCache) : AbstractListCommonizer<KotlinType, UnwrappedType>(
    singleElementCommonizerFactory = { TypeCommonizer.default(cache) }
)

interface TypeParameterListCommonizer : Commonizer<List<TypeParameter>, List<TypeParameter>> {
    companion object {
        fun default(cache: ClassifiersCache): TypeParameterListCommonizer = DefaultTypeParameterListCommonizer(cache)
    }
}

private class DefaultTypeParameterListCommonizer(cache: ClassifiersCache) :
    TypeParameterListCommonizer,
    AbstractListCommonizer<TypeParameter, TypeParameter>(
        singleElementCommonizerFactory = { TypeParameterCommonizer.default(cache) }
    )
