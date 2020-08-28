/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeParameterFactory
import org.jetbrains.kotlin.descriptors.commonizer.utils.EMPTY_CLASSIFIERS_CACHE
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockClassType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.junit.Test

class TypeParameterCommonizerTest : AbstractCommonizerTest<CirTypeParameter, CirTypeParameter>() {
    override fun createCommonizer() = TypeParameterCommonizer(EMPTY_CLASSIFIERS_CACHE)

    @Test
    fun allAreReified() = doTestSuccess(
        expected = mockTypeParam(isReified = true),
        mockTypeParam(isReified = true),
        mockTypeParam(isReified = true),
        mockTypeParam(isReified = true)
    )

    @Test
    fun allAreNotReified() = doTestSuccess(
        expected = mockTypeParam(isReified = false),
        mockTypeParam(isReified = false),
        mockTypeParam(isReified = false),
        mockTypeParam(isReified = false)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun someAreReified1() = doTestFailure(
        mockTypeParam(isReified = true),
        mockTypeParam(isReified = true),
        mockTypeParam(isReified = false)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun someAreReified2() = doTestFailure(
        mockTypeParam(isReified = false),
        mockTypeParam(isReified = false),
        mockTypeParam(isReified = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentVariance1() = doTestFailure(
        mockTypeParam(variance = Variance.IN_VARIANCE),
        mockTypeParam(variance = Variance.IN_VARIANCE),
        mockTypeParam(variance = Variance.OUT_VARIANCE)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentVariance2() = doTestFailure(
        mockTypeParam(variance = Variance.OUT_VARIANCE),
        mockTypeParam(variance = Variance.OUT_VARIANCE),
        mockTypeParam(variance = Variance.INVARIANT)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentUpperBounds1() = doTestFailure(
        mockTypeParam(upperBounds = listOf("kotlin.String")),
        mockTypeParam(upperBounds = listOf("kotlin.String")),
        mockTypeParam(upperBounds = listOf("kotlin.Int"))
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentUpperBounds2() = doTestFailure(
        mockTypeParam(upperBounds = listOf("kotlin.String", "kotlin.Int")),
        mockTypeParam(upperBounds = listOf("kotlin.String", "kotlin.Int")),
        mockTypeParam(upperBounds = listOf("kotlin.String"))
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentUpperBounds3() = doTestFailure(
        mockTypeParam(upperBounds = listOf("kotlin.String", "kotlin.Int")),
        mockTypeParam(upperBounds = listOf("kotlin.String", "kotlin.Int")),
        mockTypeParam(upperBounds = listOf("kotlin.Int", "kotlin.String"))
    )

    internal companion object {
        fun mockTypeParam(
            name: String = "T",
            isReified: Boolean = false,
            variance: Variance = Variance.INVARIANT,
            upperBounds: List<String> = listOf("kotlin.Any")
        ) = CirTypeParameterFactory.create(
            annotations = emptyList(),
            name = Name.identifier(name),
            isReified = isReified,
            variance = variance,
            upperBounds = upperBounds.map { CirTypeFactory.create(mockClassType(it)) }
        )
    }
}
