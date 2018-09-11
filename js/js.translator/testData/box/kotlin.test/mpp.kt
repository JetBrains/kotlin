// !LANGUAGE: +MultiPlatformProjects
// EXPECTED_REACHABLE_NODES: 1328
// IGNORE_BACKEND: JS_IR

// FILE: lib.kt
import kotlin.test.Test

expect class PlatformTest {
    @Test fun platformTest()
}

// FILE: main.kt
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
