import kotlin.test.*
import test1.*
import test2.*

@Test
fun runTest() {
    assertEquals(117, bar())
    assertEquals(40, baz())
}