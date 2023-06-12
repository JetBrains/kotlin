// EXPECTED_REACHABLE_NODES: 1737
// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS

import common.*
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

open class A {
    @Test fun foo(): String {
        return "promise"
    }

    @Test fun bar() = "future"
}

interface WithBefore {
    @BeforeTest fun before() {
        call("before")
    }
}

interface WithAfter {
    @AfterTest fun after() {
        call("after")
    }
}

class B: A(), WithBefore

class C: A(), WithAfter

class D: A(), WithBefore, WithAfter

fun box() = checkLog {
    suite("A") {
        test("foo") {
            returned("promise")
        }
        test("bar") {
            returned("future")
        }
    }
    suite("B") {
        test("foo") {
            call("before")
            returned("promise")
        }
        test("bar") {
            call("before")
            returned("future")
        }
    }
    suite("C") {
        test("foo") {
            call("after")
            returned("promise")
        }
        test("bar") {
            call("after")
            returned("future")
        }
    }
    suite("D") {
        test("foo") {
            call("before")
            call("after")
            returned("promise")
        }
        test("bar") {
            call("before")
            call("after")
            returned("future")
        }
    }
}