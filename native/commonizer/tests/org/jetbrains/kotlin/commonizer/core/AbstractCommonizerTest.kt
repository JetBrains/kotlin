/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class AbstractCommonizerTest<T, R> {

    class ObjectsNotEqual(message: String) : Throwable(message)

    @Test(expected = IllegalCommonizerStateException::class)
    fun failOnNoVariantsSubmitted() {
        createCommonizer().result
        fail()
    }

    protected abstract fun createCommonizer(): Commonizer<T, R>

    protected open fun areEqual(a: R?, b: R?): Boolean = a == b

    protected fun doTestSuccess(expected: R, vararg variants: T) {
        check(variants.isNotEmpty())

        val commonized = createCommonizer().apply {
            variants.forEachIndexed { index, value ->
                assertTrue(commonizeWith(value), "Expected successful commonization, but failed at index $index ($value)")
            }
        }

        val actual = commonized.result
        if (!areEqual(expected, actual)) throw ObjectsNotEqual("Expected: $expected\nActual: $actual")
    }

    protected fun doTestFailure(
        vararg variants: T,
        shouldFailOnFirstVariant: Boolean = false // by default should fail on the last variant
    ) {
        check(variants.isNotEmpty())

        val failureIndex = if (shouldFailOnFirstVariant) 0 else variants.size - 1

        val commonized = createCommonizer().apply {
            variants.forEachIndexed { index, variant ->
                val result = commonizeWith(variant)
                if (index == variants.lastIndex) {
                    if (this.result == null) failInEmptyState()
                }
                if (index >= failureIndex) assertFalse(result, "Expected to fail at index $index")
                else assertTrue(result, "Expected to not fail at index $index")
            }
        }

        commonized.result
        fail()
    }
}
