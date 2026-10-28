import kotlin.test.*

@Test
fun runTest() {
    val i: I = Impl()
    assertEquals(21, callA(i) + i.b())
}
