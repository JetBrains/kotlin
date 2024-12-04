import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testNSOutputStreamToMemoryConstructor() {
    val stream: Any = NSOutputStream(toMemory = Unit)
    assertTrue(stream is NSOutputStream)
}