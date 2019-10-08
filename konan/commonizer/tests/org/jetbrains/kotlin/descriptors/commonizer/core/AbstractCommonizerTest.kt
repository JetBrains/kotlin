/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.junit.Test
import kotlin.test.*

abstract class AbstractCommonizerTest<T, R> {

    @Test(expected = IllegalCommonizerStateException::class)
    fun failOnNoVariantsSubmitted() {
        createCommonizer().result
        fail()
    }

    protected abstract fun createCommonizer(): Commonizer<T, R>

    protected open fun isEqual(a: R?, b: R?): Boolean = a == b

    protected fun doTestSuccess(expected: R, vararg variants: T) {
        check(variants.isNotEmpty())

        val commonized = createCommonizer().apply {
            variants.forEach {
                assertTrue(commonizeWith(it))
            }
        }

        val actual = commonized.result
        if (!isEqual(expected, actual)) fail("Expected: $expected\nActual: $actual")
    }

    // should fail on the last variant
    protected fun doTestFailure(vararg variants: T) {
        check(variants.isNotEmpty())

        val commonized = createCommonizer().apply {
            variants.forEachIndexed { index, variant ->
                val result = commonizeWith(variant)
                if (index == variants.size - 1) assertFalse(result) else assertTrue(result)
            }
        }

        commonized.result
        fail()
    }
}
