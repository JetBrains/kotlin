// LANGUAGE: +MultiPlatformProjects
// TARGET_FRONTEND: ClassicFrontend
// FIR status: expect/actual in one module
// KJS_WITH_FULL_RUNTIME
// SKIP_DCE_DRIVEN
// RUN_UNIT_TESTS

// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^ @Suppress("INVISIBLE_MEMBER") in _common.kt

// FILE: lib.kt
import kotlin.test.Test

expect class PlatformTest {
    @Test fun platformTest()
}

// FILE: main.kt
import common.*
import kotlin.test.Test

actual class PlatformTest {
    @Test actual fun platformTest() {}

    @Test fun someOtherTest() {}
}

fun box() = checkLog {
    suite("PlatformTest") {
        test("platformTest")
        test("someOtherTest")
    }
}
