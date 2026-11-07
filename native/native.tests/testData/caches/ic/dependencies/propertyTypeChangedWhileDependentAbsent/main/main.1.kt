import kotlin.test.*

@Test
fun runTest() {
    // x is out of the link at this step; the program reads y's property directly.
    assertEquals("s", p)
}
