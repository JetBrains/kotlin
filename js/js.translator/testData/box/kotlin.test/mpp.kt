// EXPECTED_REACHABLE_NODES: 1173
// MULTIPLATFORM

import kotlin.test.Test

expect class PlatformTest {
    @Test fun platformTest()
}

actual class PlatformTest {
    @Test actual fun platformTest() {}
}

fun box() = checkLog {
    suite("PlatformTest") {
        test("platformTest")
    }
}