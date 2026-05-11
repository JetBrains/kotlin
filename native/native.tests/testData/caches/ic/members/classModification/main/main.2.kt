import kotlin.test.*
import test.*

@Test
fun runTest() {
    val demo = Demo("a", "fixed")
    assertEquals("foo changed a", useFoo(demo))
    assertEquals("hidden changed a", useHidden(demo))
    assertEquals("demo field", useField(demo))
    assertEquals("fixed", useDefaultY(demo))
}
