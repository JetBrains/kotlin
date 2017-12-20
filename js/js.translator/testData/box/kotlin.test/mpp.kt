// EXPECTED_REACHABLE_NODES: 1173
// MULTIPLATFORM

// MODULE: lib
// FILE: lib.kt
import kotlin.test.Test

expect class PlatformTest {
    @Test fun platformTest()
}

// MODULE: main(lib)
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