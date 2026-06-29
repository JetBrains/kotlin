/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.DefaultCommonizerSettings
import org.jetbrains.kotlin.commonizer.cir.CirTypeParameter
import org.jetbrains.kotlin.commonizer.utils.MOCK_CLASSIFIERS
import org.junit.jupiter.api.Test

class TypeParameterListCommonizerTest : AbstractCommonizerTest<List<CirTypeParameter>, List<CirTypeParameter>?>() {

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
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        )
    )

    @Test
    fun mismatchedParameterListSize1() = doTestFailure<IllegalCommonizerStateException>(
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        emptyList()
    )

    @Test
    fun mismatchedParameterListSize2() = doTestFailure<IllegalCommonizerStateException>(
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence"
        )
    )

    @Test
    fun mismatchedParameterListSize3() = doTestFailure<IllegalCommonizerStateException>(
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence",
            "Q" to "org/sample/Foo",
            "V" to "org/sample/Bar"
        )
    )

    @Test
    fun mismatchedParameterNames() = doTestFailure<IllegalCommonizerStateException>(
        mockTypeParams(
            "T" to "kotlin/Any",
            "R" to "kotlin/CharSequence"
        ),
        mockTypeParams(
            "T" to "kotlin/Any",
            "Q" to "kotlin/CharSequence"
        )
    )

    override fun createCommonizer() = TypeParameterListCommonizer(TypeCommonizer(MOCK_CLASSIFIERS, DefaultCommonizerSettings))

    private companion object {
        fun mockTypeParams(vararg params: Pair<String, String>): List<CirTypeParameter> {
            check(params.isNotEmpty())
            return params.map { [name, upperBounds] ->
                TypeParameterCommonizerTest.mockTypeParam(
                    name = name,
                    upperBounds = listOf(upperBounds)
                )
            }
        }
    }
}
