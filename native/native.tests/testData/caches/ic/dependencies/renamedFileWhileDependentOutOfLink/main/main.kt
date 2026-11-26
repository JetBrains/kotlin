import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(7, viaPub())
    assertEquals(7, pub())
}
