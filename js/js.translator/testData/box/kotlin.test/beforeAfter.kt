// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS

// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^ @Suppress("INVISIBLE_MEMBER") in _common.kt

import common.*
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

class Simple {
    @BeforeTest
    fun before() {
        call("before")
    }

    @AfterTest
    fun after() {
        call("after")
    }

    @Test
    fun foo() {
        call("foo")
    }

    @Test
    fun bar() {
        call("bar")
    }

    @Test
    fun withException() {
        call("withException")
        raise("some exception")
        call("never happens")
    }
}

fun box() = checkLog {
    suite("Simple") {
        test("foo") {
            call("before")
            call("foo")
            call("after")
        }
        test("bar") {
            call("before")
            call("bar")
            call("after")
        }
        test("withException") {
            call("before")
            call("withException")
            raised("some exception")
            call("after")
            caught("some exception")
        }
    }
}