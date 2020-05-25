/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.utils.EMPTY_CLASSIFIERS_CACHE
import org.junit.Test

class TypeParameterListCommonizerTest : AbstractCommonizerTest<List<CirTypeParameter>, List<CirTypeParameter>>() {

    @Test
    fun emptyValueParameters() = doTestSuccess(
        expected = emptyList(),
        emptyList(),
        emptyList(),
        emptyList()
    )

    @Test
    fun matchedParameters() = doTestSuccess(
        expected = mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        )
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize1() = doTestFailure(
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        emptyList()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize2() = doTestFailure(
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence"
        )
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize3() = doTestFailure(
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?",
            "V" to "org.sample.Bar"
        )
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterNames() = doTestFailure(
        mockTypeParams(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence"
        ),
        mockTypeParams(
            "T" to "kotlin.Any?",
            "Q" to "kotlin.CharSequence"
        )
    )

    override fun createCommonizer() = TypeParameterListCommonizer(EMPTY_CLASSIFIERS_CACHE)

    private companion object {
        fun mockTypeParams(vararg params: Pair<String, String>): List<CirTypeParameter> {
            check(params.isNotEmpty())
            return params.map { (name, returnTypeFqName) ->
                TypeParameterCommonizerTest.mockTypeParam(
                    name = name,
                    upperBounds = listOf(returnTypeFqName)
                )
            }
        }
    }
}
