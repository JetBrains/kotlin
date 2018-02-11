// EXPECTED_REACHABLE_NODES: 1173
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