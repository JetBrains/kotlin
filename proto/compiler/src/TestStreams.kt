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
    assertSuccessfulRead(1, 42, ins.readInt32())
    assertSuccessfulRead(42, 1, ins.readInt32())
    assertSuccessfulRead(2, -2, ins.readInt32())
    assertSuccessfulRead(3, -21321, ins.readInt32())
    assertSuccessfulRead(5, 42L, ins.readInt64())
    assertSuccessfulRead(6, 1232132131212321L, ins.readInt64())
    assertSuccessfulRead(7, -2, ins.readInt64())
    assertSuccessfulRead(8, -21321L, ins.readInt64())
    assertSuccessfulRead(9, -32132132132131L, ins.readInt64())
}


fun testMinPossibleVarInts() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeInt32(1, Int.MIN_VALUE)
    outs.writeInt64(2, Long.MIN_VALUE)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(1, Int.MIN_VALUE, ins.readInt32())
    assertSuccessfulRead(2, Long.MIN_VALUE, ins.readInt64())
}

fun testMaxPossibleVarInts() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeInt32(1, Int.MAX_VALUE)
    outs.writeInt64(2, Long.MAX_VALUE)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(1, Int.MAX_VALUE, ins.readInt32())
    assertSuccessfulRead(2, Long.MAX_VALUE, ins.readInt64())
}

fun testMaxPossibleFieldNumber() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeInt64(536870911, Long.MAX_VALUE)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(536870911, Long.MAX_VALUE, ins.readInt64())
}

fun testZigZag32() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeSInt32(5, -213123)
    outs.writeSInt32(1, 3123123)
    outs.writeSInt32(4, -12345675)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(5, -213123, ins.readSInt32())
    assertSuccessfulRead(1, 3123123, ins.readSInt32())
    assertSuccessfulRead(4, -12345675, ins.readSInt32())
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
    assertSuccessfulRead(5, -213123L, ins.readSInt64())
    assertSuccessfulRead(1, 3123123L, ins.readSInt64())
    assertSuccessfulRead(4, -12345675L, ins.readSInt64())
    assertSuccessfulRead(12, -123456789012L, ins.readSInt64())
    assertSuccessfulRead(42, 483567314231L, ins.readSInt64())
}

fun testBoolean() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeBool(1, true)
    outs.writeBool(2, false)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(1, true, ins.readBool())
    assertSuccessfulRead(2, false, ins.readBool())
}

fun testEnum() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeEnum(1, WireType.END_GROUP.ordinal)
    outs.writeEnum(2, WireType.FIX_64.ordinal)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(1, WireType.END_GROUP.ordinal, ins.readEnum())
    assertSuccessfulRead(2, WireType.FIX_64.ordinal, ins.readEnum())
}

fun testFloatAndDouble() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeDouble(42, 123212.34282)
    outs.writeDouble(15, Math.PI)
    outs.writeFloat(14, -1.32131321f)

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(42, 123212.34282, ins.readDouble())
    assertSuccessfulRead(15, Math.PI, ins.readDouble())
    assertSuccessfulRead(14, -1.32131321f, ins.readFloat())
}

fun testStrings() {
    val s = ByteArrayOutputStream()
    val outs = CodedOutputStream(s)
    outs.writeString(42, "dasddasd asd ")
    outs.writeString(15, """!@#$%^&*()QWERTYUI выфвфывфы""")

    val ins = CodedInputStream(ByteArrayInputStream(s.toByteArray()))
    assertSuccessfulRead(42, "dasddasd asd ", ins.readString())
    assertSuccessfulRead(15, """!@#$%^&*()QWERTYUI выфвфывфы""", ins.readString())
}

fun <T> assertSuccessfulRead(fieldNumber: Int, value: T, field: CodedInputStream.Field<T>) {
    assert(field.fieldNumber == fieldNumber)
    assert(field.wireType == WireType.VARINT)
    assert(field.value == value)
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