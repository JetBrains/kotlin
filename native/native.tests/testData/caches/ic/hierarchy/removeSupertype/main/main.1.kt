import kotlin.test.*
import test2.*

@Test
fun runTest() {
    assertEquals("foo Changed", Derived().bar())
}
