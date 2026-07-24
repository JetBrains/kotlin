import kotlin.test.*
import test2.*

@Test
fun runTest() {
    assertEquals("foo FirstBase", Derived().bar())
}
