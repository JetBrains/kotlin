import kotlin.test.*

@Test
fun runTest() {
    assertEquals(1, callUsed())
    assertEquals(11, callM(Sub()))
    assertEquals(10, callM(K()))
}
