import kotlin.test.*

@Test
fun runTest() {
    assertEquals(3, f(object : A {}))
}
