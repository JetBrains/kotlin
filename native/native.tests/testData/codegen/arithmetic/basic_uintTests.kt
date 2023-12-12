import kotlin.test.*

@kotlin.ExperimentalUnsignedTypes
fun box(): String {
    assertEquals(UInt.MAX_VALUE, UInt.MIN_VALUE - 1u)

    return "OK"
}
