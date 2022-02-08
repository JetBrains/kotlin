import library.*
import kotlinx.cinterop.*
import kotlin.test.*

fun main() {
    assertNotNull(s.ptr)
    assertNotNull(u.ptr)
    assertNotNull(array)

    assertEquals("initial", getContent(s.ptr)?.toKString())
    setContent(s.ptr, "yo")
    val ptr = getContent(s.ptr)
    assertEquals("yo", ptr?.toKString())

    assertEquals(0.0, getDouble(u.ptr))
    setDouble(u.ptr, Double.MIN_VALUE)
    assertEquals(Double.MIN_VALUE, getDouble(u.ptr))

    for (i in 0 until arrayLength()) {
        assertEquals(0x0, array[i])
    }
    setArrayValue(array, 0x1)
    for (i in 0 until arrayLength()) {
        assertEquals(0x1, array[i])
    }
}