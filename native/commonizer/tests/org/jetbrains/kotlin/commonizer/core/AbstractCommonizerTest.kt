/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class AbstractCommonizerTest<T, R> {

    class ObjectsNotEqual(message: String) : Throwable(message)

    @Test
    fun failOnNoVariantsSubmitted() {
        assertThrows<IllegalCommonizerStateException> {
            createCommonizer().result
        }
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

    protected inline fun <reified E : Throwable> doTestFailure(
        vararg variants: T,
        shouldFailOnFirstVariant: Boolean = false // by default should fail on the last variant
    ) {
        assertThrows<E> { doTestFailureImpl(variants, shouldFailOnFirstVariant) }
    }

    protected fun doTestFailureImpl(
        variants: Array<out T>,
        shouldFailOnFirstVariant: Boolean // by default should fail on the last variant
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
