// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS
// ES_MODULES

// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^ @Suppress("INVISIBLE_MEMBER") in _common.kt

// FILE: test.kt
import common.call
import kotlin.test.Test

class Simple {
    @Test fun foo() {
        call("foo")
    }
}

// FILE: box.kt
import common.*

fun box() = checkLog {
    suite("Simple") {
        test("foo") {
            call("foo")
        }
    }
}