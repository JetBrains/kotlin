// EXPECTED_REACHABLE_NODES: 1698
// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS

// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^ @Suppress("INVISIBLE_MEMBER") in _common.kt

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