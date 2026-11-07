import kotlin.test.*

@Test
fun runTest() {
    B.b
    A.p
    // With -Xcompanion-blocks all companions initialize eagerly in declaration order: A then B.
    assertEquals("AB", log)
}
