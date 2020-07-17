/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.core.CirTestValueParameter.Companion.areEqual
import org.jetbrains.kotlin.descriptors.commonizer.utils.EMPTY_CLASSIFIERS_CACHE
import org.junit.Test

class ValueParameterListCommonizerTest : AbstractCommonizerTest<List<CirValueParameter>, List<CirValueParameter>>() {

    @Test
    fun emptyValueParameters() = doTestSuccess(
        expected = emptyList(),
        emptyList(),
        emptyList(),
        emptyList()
    )

    @Test
    fun matchedParameters() = doTestSuccess(
        expected = mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        )
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize1() = doTestFailure(
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        emptyList()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize2() = doTestFailure(
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int"
        )
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize3() = doTestFailure(
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo",
            "d" to "org.sample.Bar"
        )
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterNames1() = doTestFailure(
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a1" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        )
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterNames2() = doTestFailure(
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c1" to "org.sample.Foo"
        )
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterTypes() = doTestFailure(
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        mockValueParams(
            "a" to "kotlin.Int",
            "b" to "kotlin.String",
            "c" to "org.sample.Bar"
        )
    )

    override fun createCommonizer() = ValueParameterListCommonizer(EMPTY_CLASSIFIERS_CACHE)

    override fun isEqual(a: List<CirValueParameter>?, b: List<CirValueParameter>?): Boolean {
        if (a === b)
            return true
        else if (a == null || b == null || a.size != b.size)
            return false

        for (i in a.indices) {
            if (!areEqual(EMPTY_CLASSIFIERS_CACHE, a[i], b[i]))
                return false
        }

        return true
    }

    private companion object {
        fun mockValueParams(vararg params: Pair<String, String>): List<CirValueParameter> {
            check(params.isNotEmpty())
            return params.map { (name, returnTypeFqName) ->
                ValueParameterCommonizerTest.mockValueParam(
                    name = name,
                    returnTypeFqName = returnTypeFqName
                )
            }
        }
    }
}
