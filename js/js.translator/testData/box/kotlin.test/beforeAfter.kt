// EXPECTED_REACHABLE_NODES: 1183
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
        exception("some exception")
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
            call("after")
            exception("some exception")
        }
    }
}