
@file:OptIn(ExperimentalForeignApi::class)
import kotlin.test.*
import kotlinx.cinterop.*

fun convertIntToShortOrNull(i: Int, b: Boolean): Short? = if (b) i.convert() else null
fun narrowIntToShortOrNull(i: Int, b: Boolean): Short? = if (b) i.narrow() else null
fun signExtendShortToIntOrNull(i: Short, b: Boolean): Int? = if (b) i.signExtend() else null

fun box(): String {
    assertNull(convertIntToShortOrNull(0, false))
    assertEquals(1, convertIntToShortOrNull(1, true))

    assertNull(narrowIntToShortOrNull(2, false))
    assertEquals(3, narrowIntToShortOrNull(3, true))

    assertNull(signExtendShortToIntOrNull(4, false))
    assertEquals(5, signExtendShortToIntOrNull(5, true))

    return "OK"
}
