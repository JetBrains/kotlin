// EXPECTED_REACHABLE_NODES: 1698
// KJS_WITH_FULL_RUNTIME

import common.*
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