import kotlin.test.*
import test.*

@Test
fun runTest() {
    val demo = Demo("a")
    assertEquals("foo changed a", useFoo(demo))
    assertEquals("hidden changed a", useHidden(demo))
    assertEquals("demo field changed", useField(demo))
    assertEquals("updated", useDefaultY(demo))
}
