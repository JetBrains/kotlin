// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1340
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