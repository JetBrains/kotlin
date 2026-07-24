import kotlin.test.*
import test1.*
import test2.*

@Test
fun runTest() {
    assertEquals(40, foo())
    assertEquals(50, bar())
}
