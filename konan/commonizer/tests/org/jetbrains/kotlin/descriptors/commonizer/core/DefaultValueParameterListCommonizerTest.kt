/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ValueParameter
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

@TypeRefinement
class DefaultValueParameterListCommonizerTest : AbstractCommonizerTest<List<ValueParameterDescriptor>, List<ValueParameter>>() {

    @Test
    fun emptyValueParameters() = doTestSuccess(
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList()
    )

    @Test
    fun matchedParameters() = doTestSuccess(
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams()
    )

    @Test(expected = IllegalStateException::class)
    fun mismatchedParameterListSize1() = doTestFailure(
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        emptyList()
    )

    @Test(expected = IllegalStateException::class)
    fun mismatchedParameterListSize2() = doTestFailure(
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int"
        ).toMockParams()
    )

    @Test(expected = IllegalStateException::class)
    fun mismatchedParameterListSize3() = doTestFailure(
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo",
            "d" to "org.sample.Bar"
        ).toMockParams()
    )

    @Test(expected = IllegalStateException::class)
    fun mismatchedParameterNames1() = doTestFailure(
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a1" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams()
    )

    @Test(expected = IllegalStateException::class)
    fun mismatchedParameterNames2() = doTestFailure(
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c1" to "org.sample.Foo"
        ).toMockParams()
    )

    @Test(expected = IllegalStateException::class)
    fun mismatchedParameterTypes() = doTestFailure(
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.String",
            "b" to "kotlin.Int",
            "c" to "org.sample.Foo"
        ).toMockParams(),
        create(
            "a" to "kotlin.Int",
            "b" to "kotlin.String",
            "c" to "org.sample.Bar"
        ).toMockParams()
    )

    override fun createCommonizer() = ValueParameterListCommonizer.default()

    private companion object {
        fun create(vararg params: Pair<String, String>): List<ValueParameter> {
            check(params.isNotEmpty())
            return params.map { (name, returnTypeFqName) ->
                DefaultValueParameterCommonizerTest.create(
                    name = name,
                    returnTypeFqName = returnTypeFqName
                )
            }
        }

        fun List<ValueParameter>.toMockParams() = DefaultValueParameterCommonizerTest.run {
            mapIndexed { index, param -> param.toMockParam(index) }
        }
    }
}
