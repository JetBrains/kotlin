import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testConstructorReturnsNull() {
    assertFailsWith<NullPointerException>() {
        TestConstructorReturnsNull()
    }
}