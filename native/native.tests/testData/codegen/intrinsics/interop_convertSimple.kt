
@file:OptIn(ExperimentalForeignApi::class)
import kotlin.test.*
import kotlinx.cinterop.*

fun box(): String {
    assertEquals(1, 257.convert<Byte>())
    assertEquals(255u, (-1).convert<UByte>())
    assertEquals(0, Long.MIN_VALUE.narrow<Int>())
    assertEquals(-1, Long.MAX_VALUE.narrow<Short>())
    assertEquals(-1L, (-1).signExtend<Long>())

    return "OK"
}
