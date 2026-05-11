import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertEquals(2, useFoo())
    assertEquals(200, useInlineFoo())
    assertEquals(20, useNestedBar())
}
