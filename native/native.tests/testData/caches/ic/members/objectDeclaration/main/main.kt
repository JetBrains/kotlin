import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(1, useFoo())
    assertEquals(10, useInlineFoo())
}
