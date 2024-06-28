/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.reflection

import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.typeOf
import kotlin.test.*

class KTypeProjectionTest {
    @Test
    fun constructorArgumentsValidation() {
        assertFailsWith<IllegalArgumentException> { KTypeProjection(null, typeOf<Int>()) }
        for (variance in KVariance.entries) {
            assertFailsWith<IllegalArgumentException> { KTypeProjection(variance, null) }.let { e ->
                assertTrue(variance.toString() in e.message!!)
            }
        }
    }
}
