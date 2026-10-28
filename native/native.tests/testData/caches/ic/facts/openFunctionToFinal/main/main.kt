import kotlin.test.*

@Test
fun runTest() {
    assertEquals(12, call(C()) + callSub() + other())
    assertEquals(1, call(Sub()))
}
