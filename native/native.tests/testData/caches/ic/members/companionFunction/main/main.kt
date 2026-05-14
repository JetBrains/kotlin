import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(1, useFoo())
    assertEquals(100, useInlineFoo())
    assertEquals(10, useNestedBar())
}
