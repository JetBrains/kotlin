/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.TypeParameter
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

@TypeRefinement
class DefaultTypeParameterListCommonizerTest : AbstractCommonizerTest<List<TypeParameterDescriptor>, List<TypeParameter>>() {

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
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ),
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams(),
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams(),
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize1() = doTestFailure(
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams(),
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams(),
        emptyList()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize2() = doTestFailure(
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams(),
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams(),
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence"
        ).toMockParams()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterListSize3() = doTestFailure(
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams(),
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?"
        ).toMockParams(),
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence",
            "Q" to "org.sample.Foo?",
            "V" to "org.sample.Bar"
        ).toMockParams()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun mismatchedParameterNames() = doTestFailure(
        create(
            "T" to "kotlin.Any?",
            "R" to "kotlin.CharSequence"
        ).toMockParams(),
        create(
            "T" to "kotlin.Any?",
            "Q" to "kotlin.CharSequence"
        ).toMockParams()
    )

    override fun createCommonizer() = TypeParameterListCommonizer.default()

    private companion object {
        fun create(vararg params: Pair<String, String>): List<TypeParameter> {
            check(params.isNotEmpty())
            return params.map { (name, returnTypeFqName) ->
                DefaultTypeParameterCommonizerTest.create(
                    name = name,
                    upperBounds = listOf(returnTypeFqName)
                )
            }
        }

        fun List<TypeParameter>.toMockParams() = DefaultTypeParameterCommonizerTest.run {
            mapIndexed { index, param -> param.toMockParam(index) }
        }
    }
}
