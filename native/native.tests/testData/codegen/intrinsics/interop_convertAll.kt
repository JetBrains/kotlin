
@file:OptIn(ExperimentalForeignApi::class)
import kotlin.test.*
import kotlinx.cinterop.*

fun box(): String {
    val values = mutableListOf<Long>()
    for (value in listOf(
            0L,
            Byte.MIN_VALUE.toLong(), Byte.MAX_VALUE.toLong(), UByte.MAX_VALUE.toLong(),
            Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong(), UShort.MAX_VALUE.toLong(),
            Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong(), UInt.MAX_VALUE.toLong(),
            Long.MIN_VALUE.toLong(), Long.MAX_VALUE.toLong(), ULong.MAX_VALUE.toLong()
    )) {
        values.add(value - 1)
        values.add(value)
        values.add(value + 1)
    }

    for (value in values) {
        testConvertAll(value.toByte())
        testConvertAll(value.toUByte())
        testConvertAll(value.toShort())
        testConvertAll(value.toUShort())
        testConvertAll(value.toInt())
        testConvertAll(value.toUInt())
        testConvertAll(value.toLong())
        testConvertAll(value.toULong())
    }
    return "OK"
}

fun testConvertAll(value: Byte) {
    assertEquals(value.toByte(), value.convert<Byte>())
    assertEquals(value.toUByte(), value.convert<UByte>())
    assertEquals(value.toShort(), value.convert<Short>())
    assertEquals(value.toUShort(), value.convert<UShort>())
    assertEquals(value.toInt(), value.convert<Int>())
    assertEquals(value.toUInt(), value.convert<UInt>())
    assertEquals(value.toLong(), value.convert<Long>())
    assertEquals(value.toULong(), value.convert<ULong>())

    assertEquals(value.toByte(), value.narrow<Byte>())

    assertEquals(value.toByte(), value.signExtend<Byte>())
    assertEquals(value.toShort(), value.signExtend<Short>())
    assertEquals(value.toInt(), value.signExtend<Int>())
    assertEquals(value.toLong(), value.signExtend<Long>())
}

fun testConvertAll(value: Short) {
    assertEquals(value.toByte(), value.convert<Byte>())
    assertEquals(value.toUByte(), value.convert<UByte>())
    assertEquals(value.toShort(), value.convert<Short>())
    assertEquals(value.toUShort(), value.convert<UShort>())
    assertEquals(value.toInt(), value.convert<Int>())
    assertEquals(value.toUInt(), value.convert<UInt>())
    assertEquals(value.toLong(), value.convert<Long>())
    assertEquals(value.toULong(), value.convert<ULong>())

    assertEquals(value.toByte(), value.narrow<Byte>())
    assertEquals(value.toShort(), value.narrow<Short>())

    assertEquals(value.toShort(), value.signExtend<Short>())
    assertEquals(value.toInt(), value.signExtend<Int>())
    assertEquals(value.toLong(), value.signExtend<Long>())
}

fun testConvertAll(value: Int) {
    assertEquals(value.toByte(), value.convert<Byte>())
    assertEquals(value.toUByte(), value.convert<UByte>())
    assertEquals(value.toShort(), value.convert<Short>())
    assertEquals(value.toUShort(), value.convert<UShort>())
    assertEquals(value.toInt(), value.convert<Int>())
    assertEquals(value.toUInt(), value.convert<UInt>())
    assertEquals(value.toLong(), value.convert<Long>())
    assertEquals(value.toULong(), value.convert<ULong>())

    assertEquals(value.toByte(), value.narrow<Byte>())
    assertEquals(value.toShort(), value.narrow<Short>())
    assertEquals(value.toInt(), value.narrow<Int>())

    assertEquals(value.toInt(), value.signExtend<Int>())
    assertEquals(value.toLong(), value.signExtend<Long>())
}

fun testConvertAll(value: Long) {
    assertEquals(value.toByte(), value.convert<Byte>())
    assertEquals(value.toUByte(), value.convert<UByte>())
    assertEquals(value.toShort(), value.convert<Short>())
    assertEquals(value.toUShort(), value.convert<UShort>())
    assertEquals(value.toInt(), value.convert<Int>())
    assertEquals(value.toUInt(), value.convert<UInt>())
    assertEquals(value.toLong(), value.convert<Long>())
    assertEquals(value.toULong(), value.convert<ULong>())

    assertEquals(value.toByte(), value.narrow<Byte>())
    assertEquals(value.toShort(), value.narrow<Short>())
    assertEquals(value.toInt(), value.narrow<Int>())
    assertEquals(value.toLong(), value.narrow<Long>())

    assertEquals(value.toLong(), value.signExtend<Long>())
}


fun testConvertAll(value: UByte) {
    assertEquals(value.toByte(), value.convert<Byte>())
    assertEquals(value.toUByte(), value.convert<UByte>())
    assertEquals(value.toShort(), value.convert<Short>())
    assertEquals(value.toUShort(), value.convert<UShort>())
    assertEquals(value.toInt(), value.convert<Int>())
    assertEquals(value.toUInt(), value.convert<UInt>())
    assertEquals(value.toLong(), value.convert<Long>())
    assertEquals(value.toULong(), value.convert<ULong>())
}

fun testConvertAll(value: UShort) {
    assertEquals(value.toByte(), value.convert<Byte>())
    assertEquals(value.toUByte(), value.convert<UByte>())
    assertEquals(value.toShort(), value.convert<Short>())
    assertEquals(value.toUShort(), value.convert<UShort>())
    assertEquals(value.toInt(), value.convert<Int>())
    assertEquals(value.toUInt(), value.convert<UInt>())
    assertEquals(value.toLong(), value.convert<Long>())
    assertEquals(value.toULong(), value.convert<ULong>())
}

fun testConvertAll(value: UInt) {
    assertEquals(value.toByte(), value.convert<Byte>())
    assertEquals(value.toUByte(), value.convert<UByte>())
    assertEquals(value.toShort(), value.convert<Short>())
    assertEquals(value.toUShort(), value.convert<UShort>())
    assertEquals(value.toInt(), value.convert<Int>())
    assertEquals(value.toUInt(), value.convert<UInt>())
    assertEquals(value.toLong(), value.convert<Long>())
    assertEquals(value.toULong(), value.convert<ULong>())
}

fun testConvertAll(value: ULong) {
    assertEquals(value.toByte(), value.convert<Byte>())
    assertEquals(value.toUByte(), value.convert<UByte>())
    assertEquals(value.toShort(), value.convert<Short>())
    assertEquals(value.toUShort(), value.convert<UShort>())
    assertEquals(value.toInt(), value.convert<Int>())
    assertEquals(value.toUInt(), value.convert<UInt>())
    assertEquals(value.toLong(), value.convert<Long>())
    assertEquals(value.toULong(), value.convert<ULong>())
}
