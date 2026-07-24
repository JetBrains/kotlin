import kotlin.test.*
import test2.*

@Test
fun runTest() {
    assertEquals("foo SecondBase", Derived().bar())
}
