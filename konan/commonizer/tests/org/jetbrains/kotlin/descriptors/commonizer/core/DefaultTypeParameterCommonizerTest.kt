/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CommonTypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.TypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.mockClassType
import org.jetbrains.kotlin.descriptors.commonizer.mockTypeParameter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

@TypeRefinement
class DefaultTypeParameterCommonizerTest : AbstractCommonizerTest<TypeParameterDescriptor, TypeParameter>() {
    override fun createCommonizer() = TypeParameterCommonizer.default()

    @Test
    fun allAreReified() = doTestSuccess(
        create(isReified = true),
        create(isReified = true).toMockParam(),
        create(isReified = true).toMockParam(),
        create(isReified = true).toMockParam()
    )

    @Test
    fun allAreNotReified() = doTestSuccess(
        create(isReified = false),
        create(isReified = false).toMockParam(),
        create(isReified = false).toMockParam(),
        create(isReified = false).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun someAreReified1() = doTestFailure(
        create(isReified = true).toMockParam(),
        create(isReified = true).toMockParam(),
        create(isReified = false).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun someAreReified2() = doTestFailure(
        create(isReified = false).toMockParam(),
        create(isReified = false).toMockParam(),
        create(isReified = true).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun differentVariance1() = doTestFailure(
        create(variance = Variance.IN_VARIANCE).toMockParam(),
        create(variance = Variance.IN_VARIANCE).toMockParam(),
        create(variance = Variance.OUT_VARIANCE).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun differentVariance2() = doTestFailure(
        create(variance = Variance.OUT_VARIANCE).toMockParam(),
        create(variance = Variance.OUT_VARIANCE).toMockParam(),
        create(variance = Variance.INVARIANT).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun differentUpperBounds1() = doTestFailure(
        create(upperBounds = listOf("kotlin.String")).toMockParam(),
        create(upperBounds = listOf("kotlin.String")).toMockParam(),
        create(upperBounds = listOf("kotlin.Int")).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun differentUpperBounds2() = doTestFailure(
        create(upperBounds = listOf("kotlin.String", "kotlin.Int")).toMockParam(),
        create(upperBounds = listOf("kotlin.String", "kotlin.Int")).toMockParam(),
        create(upperBounds = listOf("kotlin.String")).toMockParam()
    )

    @Test(expected = IllegalStateException::class)
    fun differentUpperBounds3() = doTestFailure(
        create(upperBounds = listOf("kotlin.String", "kotlin.Int")).toMockParam(),
        create(upperBounds = listOf("kotlin.String", "kotlin.Int")).toMockParam(),
        create(upperBounds = listOf("kotlin.Int", "kotlin.String")).toMockParam()
    )

    internal companion object {
        fun create(
            name: String = "T",
            isReified: Boolean = false,
            variance: Variance = Variance.INVARIANT,
            upperBounds: List<String> = listOf("kotlin.Any")
        ) = CommonTypeParameter(
            name = Name.identifier(name),
            isReified = isReified,
            variance = variance,
            upperBounds = upperBounds.map { mockClassType(it).unwrap() }
        )

        fun TypeParameter.toMockParam(
            index: Int = 0
        ): TypeParameterDescriptor = mockTypeParameter(
            name = name.asString(),
            index = index,
            isReified = isReified,
            variance = variance,
            upperBounds = upperBounds
        )
    }
}
