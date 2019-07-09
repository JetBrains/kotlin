/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

class PreconditionsTest() {

    @Test fun passingRequire() {
        require(true)

        var called = false
        require(true) { called = true; "some message" }
        assertFalse(called)
    }

    @Test fun failingRequire() {
        val error = assertFailsWith<IllegalArgumentException> {
            require(false)
        }
        assertNotNull(error.message)
    }

    @Test fun failingRequireWithLazyMessage() {
        val error = assertFailsWith<IllegalArgumentException> {
            require(false) { "Hello" }
        }
        assertEquals("Hello", error.message)
    }

    @Test fun passingCheck() {
        check(true)

        var called = false
        check(true) { called = true; "some message" }
        assertFalse(called)
    }

    @Test fun failingCheck() {
        val error = assertFailsWith<IllegalStateException> {
            check(false)
        }
        assertNotNull(error.message)
    }

    @Test fun failingCheckWithLazyMessage() {
        val error = assertFailsWith<IllegalStateException> {
            check(false) { "Hello" }
        }
        assertEquals("Hello", error.message)
    }

    @Test fun requireNotNull() {
        val s1: String? = "S1"
        val r1: String = requireNotNull(s1)
        assertEquals("S1", r1)
    }

    @Test fun requireNotNullFails() {
        assertFailsWith<IllegalArgumentException> {
            val s2: String? = null
            requireNotNull(s2)
        }
    }

    @Test fun requireNotNullWithLazyMessage() {
        val error = assertFailsWith<IllegalArgumentException> {
            val obj: Any? = null
            requireNotNull(obj) { "Message" }
        }
        assertEquals("Message", error.message)

        var lazyCalled: Boolean = false
        requireNotNull("not null") {
            lazyCalled = true
            "Message"
        }
        assertFalse(lazyCalled, "Message is not evaluated if the condition is met")
    }

    @Test fun checkNotNull() {
        val s1: String? = "S1"
        val r1: String = checkNotNull(s1)
        assertEquals("S1", r1)
    }

    @Test fun checkNotNullFails() {
        assertFailsWith<IllegalStateException> {
            val s2: String? = null
            checkNotNull(s2)
        }
    }

    @Test fun error() {
        val error = assertFailsWith<IllegalStateException> {
            error("There was a problem")
        }
        assertEquals("There was a problem", error.message)
    }

}