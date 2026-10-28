import kotlin.test.*

@Test
fun runTest() {
    val b: Base = Derived()
    assertEquals(112, call(b) + other() + b.g())
}
