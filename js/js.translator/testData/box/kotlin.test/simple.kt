// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1330
import kotlin.test.Test

class Simple {
    @Test fun foo() {
        call("foo")
    }
}

fun box() = checkLog {
    suite("Simple") {
        test("foo") {
            call("foo")
        }
    }
}