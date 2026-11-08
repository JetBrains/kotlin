import kotlin.test.*

// Must never run in a consumer's binary: an external module's @Test functions belong to the
// module's own test binaries only. Fails on purpose so that a build that (incorrectly)
// registers it visibly fails the binary instead of silently gaining a passing test.
@Test
fun extTest() {
    assertEquals("consumer-only", "ext-test-ran")
}

// The ordinary API a consumer may use.
fun extValue(): Int = 9
