/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

class AssertionsJVMTest() {

    @Test fun passingAssert() {
        assert(true)
        var called = false
        assert(true) { called = true; "some message" }

        assertFalse(called)
    }


    @Test fun failingAssert() {
        val error = assertFailsWith<AssertionError> {
            assert(false)
        }
        assertEquals("Assertion failed", error.message)
    }


    @Test fun failingAssertWithMessage() {
        val error = assertFailsWith<AssertionError> {
            assert(false) { "Hello" }
        }
        assertEquals("Hello", error.message)
    }

}