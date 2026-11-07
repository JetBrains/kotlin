import kotlin.test.*

@Test
fun runTest() {
    B.b
    A.p
    // Without -Xcompanion-blocks the companions initialize on first access: B's then A's.
    assertEquals("BA", log)
}
