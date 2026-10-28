import kotlin.test.*

@Test
fun runTest() {
    assertEquals(5, read(Derived()))
    assertEquals(1, Derived().a)
}
