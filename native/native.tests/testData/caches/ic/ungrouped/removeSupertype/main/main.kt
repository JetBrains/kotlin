import kotlin.test.*
import test2.*

@Test
fun runTest() {
    assertEquals("foo Base", Derived().bar())
}
