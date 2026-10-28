import kotlin.test.*

@Test
fun runTest() {
    assertEquals("BLUE", blue().name)
    assertEquals(1, blue().ordinal)
    assertEquals("b", name(blue()))
    assertEquals(1, ordinal(blue()))
}
