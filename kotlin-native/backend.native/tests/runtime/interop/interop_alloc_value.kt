package runtime.interop.interop_alloc_value

import kotlinx.cinterop.*
import kotlin.test.*

@Test
fun testBoolean() = memScoped {
    assertEquals(true, alloc(true).value)
    assertEquals(false, alloc(false).value)
}

@Test
fun testByte() = memScoped<Unit> {
    assertEquals(Byte.MIN_VALUE, alloc(Byte.MIN_VALUE).value)
    assertEquals(Byte.MAX_VALUE, alloc(Byte.MAX_VALUE).value)
    assertEquals(0.toByte(), alloc(0.toByte()).value)
    assertEquals(1.toByte(), alloc(1.toByte()).value)
    assertEquals(1, alloc<Byte>(1).value)
}

@Test
fun testShort() = memScoped<Unit> {
    assertEquals(Short.MIN_VALUE, alloc(Short.MIN_VALUE).value)
    assertEquals(Short.MAX_VALUE, alloc(Short.MAX_VALUE).value)
    assertEquals(0.toShort(), alloc(0.toShort()).value)
    assertEquals(2.toShort(), alloc(2.toShort()).value)
    assertEquals(2, alloc<Short>(2).value)
}

@Test
fun testInt() = memScoped<Unit> {
    assertEquals(Int.MIN_VALUE, alloc(Int.MIN_VALUE).value)
    assertEquals(Int.MAX_VALUE, alloc(Int.MAX_VALUE).value)
    assertEquals(0.toInt(), alloc(0.toInt()).value)
    assertEquals(3.toInt(), alloc(3.toInt()).value)
    assertEquals(3, alloc<Int>(3).value)
}

@Test
fun testLong() = memScoped<Unit> {
    assertEquals(Long.MIN_VALUE, alloc(Long.MIN_VALUE).value)
    assertEquals(Long.MAX_VALUE, alloc(Long.MAX_VALUE).value)
    assertEquals(0L, alloc(0L).value)
    assertEquals(4L, alloc(4L).value)
    assertEquals(4, alloc<Long>(4).value)
}

@Test
fun testUByte() = memScoped<Unit> {
    assertEquals(UByte.MIN_VALUE, alloc(UByte.MIN_VALUE).value)
    assertEquals(UByte.MAX_VALUE, alloc(UByte.MAX_VALUE).value)
    assertEquals(5.toUByte(), alloc(5.toUByte()).value)
    assertEquals(5u, alloc<UByte>(5u).value)
}

@Test
fun testUShort() = memScoped<Unit> {
    assertEquals(UShort.MIN_VALUE, alloc(UShort.MIN_VALUE).value)
    assertEquals(UShort.MAX_VALUE, alloc(UShort.MAX_VALUE).value)
    assertEquals(6.toUShort(), alloc(6.toUShort()).value)
    assertEquals(6u, alloc<UShort>(6u).value)
}

@Test
fun testUInt() = memScoped<Unit> {
    assertEquals(UInt.MIN_VALUE, alloc(UInt.MIN_VALUE).value)
    assertEquals(UInt.MAX_VALUE, alloc(UInt.MAX_VALUE).value)
    assertEquals(7.toUInt(), alloc(7.toUInt()).value)
    assertEquals(7u, alloc<UInt>(7u).value)
}

@Test
fun testULong() = memScoped<Unit> {
    assertEquals(ULong.MIN_VALUE, alloc(ULong.MIN_VALUE).value)
    assertEquals(ULong.MAX_VALUE, alloc(ULong.MAX_VALUE).value)
    assertEquals(8uL, alloc(8uL).value)
    assertEquals(8u, alloc<ULong>(8u).value)
}

@Test
fun testFloat() = memScoped<Unit> {
    assertEquals(Float.MIN_VALUE, alloc(Float.MIN_VALUE).value)
    assertEquals(Float.MAX_VALUE, alloc(Float.MAX_VALUE).value)
    assertEquals(0.0f, alloc(0.0f).value)
    assertEquals(9.0f, alloc(9.0f).value)
    assertEquals(9.0f, alloc<Float>(9.0f).value)
}

@Test
fun testDouble() = memScoped<Unit> {
    assertEquals(Double.MIN_VALUE, alloc(Double.MIN_VALUE).value)
    assertEquals(Double.MAX_VALUE, alloc(Double.MAX_VALUE).value)
    assertEquals(0.0, alloc(0.0).value)
    assertEquals(10.0, alloc(10.0).value)
    assertEquals(10.0, alloc<Double>(10.0).value)
}
