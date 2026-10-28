import kotlin.test.*

@Test
fun runTest() {
    assertEquals(4, Leaf().w)
    assertTrue(isB(Leaf()))
    assertEquals(5, Leaf().y + Leaf().z)
}
