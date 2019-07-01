/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.properties.delegation.lazy

import kotlin.test.*

class LazyValTest {
    var result = 0
    val a by lazy {
        ++result
    }

    @Test fun doTest() {
        a
        assertTrue(a == 1, "fail: initializer should be invoked only once")
    }
}

class UnsafeLazyValTest {
    var result = 0
    val a by lazy(LazyThreadSafetyMode.NONE) {
        ++result
    }

    @Test fun doTest() {
        a
        assertTrue(a == 1, "fail: initializer should be invoked only once")
    }
}

class NullableLazyValTest {
    var resultA = 0
    var resultB = 0

    val a: Int? by lazy { resultA++; null}
    val b by lazy { foo() }

    @Test fun doTest() {
        a
        b

        assertTrue(a == null, "fail: a should be null")
        assertTrue(b == null, "fail: b should be null")
        assertTrue(resultA == 1, "fail: initializer for a should be invoked only once")
        assertTrue(resultB == 1, "fail: initializer for b should be invoked only once")
    }

    fun foo(): String? {
        resultB++
        return null
    }
}

class UnsafeNullableLazyValTest {
    var resultA = 0
    var resultB = 0

    val a: Int? by lazy(LazyThreadSafetyMode.NONE) { resultA++; null}
    val b by lazy(LazyThreadSafetyMode.NONE) { foo() }

    @Test fun doTest() {
        a
        b

        assertTrue(a == null, "fail: a should be null")
        assertTrue(b == null, "fail: a should be null")
        assertTrue(resultA == 1, "fail: initializer for a should be invoked only once")
        assertTrue(resultB == 1, "fail: initializer for b should be invoked only once")
    }

    fun foo(): String? {
        resultB++
        return null
    }
}

class IdentityEqualsIsUsedToUnescapeLazyValTest {
    var equalsCalled = 0
    private val a by lazy { ClassWithCustomEquality { equalsCalled++ } }

    @Test fun doTest() {
        a
        a
        assertTrue(equalsCalled == 0, "fail: equals called $equalsCalled times.")
    }
}

private class ClassWithCustomEquality(private val onEqualsCalled: () -> Unit) {
    override fun equals(other: Any?): Boolean {
        onEqualsCalled()
        return super.equals(other)
    }
}