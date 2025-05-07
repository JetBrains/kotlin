/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.reflection

import test.*
import kotlin.reflect.*
import kotlin.test.*

class KClassTest {

    @Test
    fun className() {
        assertEquals("KClassTest", KClassTest::class.simpleName)
        testExceptOn(TestPlatform.Jvm) {
            assertEquals(null, object {}::class.simpleName) // doesn't work as documented in JDK < 9, see KT-23072
        }
    }

    @Test
    fun extendsKClassifier() {
        assertStaticAndRuntimeTypeIs<KClassifier>(KClassTest::class)
    }

    @Test
    fun isInstanceCastSafeCast() {
        fun <T : Any> checkIsInstance(kclass: KClass<T>, value: Any?, expectedResult: Boolean) {
            if (expectedResult) {
                assertTrue(kclass.isInstance(value))
                assertSame(value, kclass.safeCast(value))
                assertSame(value, kclass.cast(value))
            } else {
                assertFalse(kclass.isInstance(value))
                assertNull(kclass.safeCast(value))
                assertFailsWith<ClassCastException> { kclass.cast(value) }
            }
        }

        val numberClass = Number::class
        checkIsInstance(numberClass, 1, true)
        checkIsInstance(numberClass, 1.0, true)
        checkIsInstance(numberClass, null, false)
        checkIsInstance(numberClass, "42", false)
    }

    @Test
    fun testKClassEquals() {
        assertFalse(Nothing::class == KClassTest::class)
        assertFalse(KClassTest::class == Nothing::class)

        assertFalse(String::class == Nothing::class)
        assertFalse(Nothing::class == String::class)

        assertFalse(Unit::class == Nothing::class)
        assertFalse(Nothing::class == Unit::class)

        assertFalse(KClassTest::class == String::class)
        assertFalse(String::class == KClassTest::class)

        assertTrue(Nothing::class == Nothing::class)
        assertTrue(Unit::class == Unit::class)
        assertTrue(String::class == String::class)
        assertTrue(KClassTest::class == KClassTest::class)
    }
}
