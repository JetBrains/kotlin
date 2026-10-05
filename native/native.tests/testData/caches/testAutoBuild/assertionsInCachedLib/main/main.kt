import kotlin.test.*

@Test
fun runTest() {
    assertFailsWith<AssertionError> { failingAssertion() }
}
