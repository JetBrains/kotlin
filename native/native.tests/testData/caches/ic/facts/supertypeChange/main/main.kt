import kotlin.test.*

@Test
fun runTest() {
    assertEquals(4, Leaf().w)
    assertFalse(isB(Leaf()))
    assertEquals(1, Leaf().x)
}
