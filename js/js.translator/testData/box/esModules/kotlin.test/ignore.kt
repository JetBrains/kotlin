// EXPECTED_REACHABLE_NODES: 1709
// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS
// ES_MODULES

// FILE: test.kt
import kotlin.test.Test
import kotlin.test.Ignore

class A {
    @Test
    fun foo() {
    }

    @Ignore
    @Test
    fun bar() {
    }

    @Ignore
    class B {
        @Test
        fun foo() {
        }

        @Ignore
        @Test
        fun bar() {
        }
    }
}

@Ignore
class C {
    @Test
    fun foo() {
    }

    @Ignore
    @Test
    fun bar() {
    }
}

// FILE: box.kt
import common.*

fun box() = checkLog {
    suite("A") {
        test("foo")
        test("bar", true)
        suite("B", true) {
            test("foo")
            test("bar", true)
        }
    }
    suite("C", true) {
        test("foo")
        test("bar", true)
    }
}