import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(2, useFoo())
    assertEquals(20, useInlineFoo())
}
