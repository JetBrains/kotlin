import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Created by user on 7/7/16.
 */

fun testTrivialPositiveVarInts() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeInt32(1, 42)
    outs.writeInt32(16, 1)
    outs.writeInt32(2, -2)
    outs.writeInt32(3, -21321)
    outs.writeInt64(5, 42L)
    outs.writeInt64(6, 1232132131212321L)
    outs.writeInt64(7, -2)
    outs.writeInt64(8, -21321L)
    outs.writeInt64(9, -32132132132131L)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(42, ins.readInt32(1))
    assertSuccessfulRead(1, ins.readInt32(16))
    assertSuccessfulRead(-2, ins.readInt32(2))
    assertSuccessfulRead(-21321, ins.readInt32(3))
    assertSuccessfulRead(42L, ins.readInt64(5))
    assertSuccessfulRead(1232132131212321L, ins.readInt64(6))
    assertSuccessfulRead(-2, ins.readInt64(7))
    assertSuccessfulRead(-21321L, ins.readInt64(8))
    assertSuccessfulRead(-32132132132131L, ins.readInt64(9))
}


fun testMinPossibleVarInts() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeInt32(1, Int.MIN_VALUE)
    outs.writeInt64(2, Long.MIN_VALUE)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(Int.MIN_VALUE, ins.readInt32(1))
    assertSuccessfulRead(Long.MIN_VALUE, ins.readInt64(2))
}

fun testMaxPossibleVarInts() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeInt32(1, Int.MAX_VALUE)
    outs.writeInt64(2, Long.MAX_VALUE)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(Int.MAX_VALUE, ins.readInt32(1))
    assertSuccessfulRead(Long.MAX_VALUE, ins.readInt64(2))
}

fun testMaxPossibleFieldNumber() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeInt64(536870911, Long.MAX_VALUE)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(Long.MAX_VALUE, ins.readInt64(536870911))
}

fun testZigZag32() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeSInt32(5, -213123)
    outs.writeSInt32(1, 3123123)
    outs.writeSInt32(4, -12345675)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(-213123, ins.readSInt32(5))
    assertSuccessfulRead(3123123, ins.readSInt32(1))
    assertSuccessfulRead(-12345675, ins.readSInt32(4))
}

fun testZigZag64() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeSInt64(5, -213123L)
    outs.writeSInt64(1, 3123123L)
    outs.writeSInt64(4, -12345675L)
    outs.writeSInt64(12, -123456789012L)
    outs.writeSInt64(42, 483567314231L)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(-213123L, ins.readSInt64(5))
    assertSuccessfulRead(3123123L, ins.readSInt64(1))
    assertSuccessfulRead(-12345675L, ins.readSInt64(4))
    assertSuccessfulRead(-123456789012L, ins.readSInt64(12))
    assertSuccessfulRead(483567314231L, ins.readSInt64(42))
}

fun testBoolean() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeBool(1, true)
    outs.writeBool(2, false)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(true, ins.readBool(1))
    assertSuccessfulRead(false, ins.readBool(2))
}

fun testEnum() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeEnum(1, WireType.END_GROUP.ordinal)
    outs.writeEnum(2, WireType.FIX_64.ordinal)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(WireType.END_GROUP.ordinal, ins.readEnum(1))
    assertSuccessfulRead(WireType.FIX_64.ordinal, ins.readEnum(2))
}

fun testFloatAndDouble() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeDouble(42, 123212.34282)
    outs.writeDouble(15, Math.PI)
    outs.writeFloat(14, -1.32131321f)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(123212.34282, ins.readDouble(42))
    assertSuccessfulRead(Math.PI, ins.readDouble(15))
    assertSuccessfulRead(-1.32131321f, ins.readFloat(14))
}

fun testStrings() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeString(42, "dasddasd asd ")
    outs.writeString(15, """!@#$%^&*()QWERTYUI выфвфывфы""")

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead("dasddasd asd ", ins.readString(42))
    assertSuccessfulRead("""!@#$%^&*()QWERTYUI выфвфывфы""", ins.readString(15))
}

fun <T> assertSuccessfulRead(value: T, actualValue: T) {
    assert(actualValue == value)
}

fun main(args: Array<String>) {
    testTrivialPositiveVarInts()
    testMinPossibleVarInts()
    testMaxPossibleVarInts()
    testMaxPossibleFieldNumber()
    testZigZag32()
    testZigZag64()
    testBoolean()
    testEnum()
    testFloatAndDouble()
    testStrings()
    print("OK")
}