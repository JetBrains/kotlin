// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1857

package foo

open class AByte(vararg val x: Byte)
class BByte : AByte()

open class AShort(vararg val x: Short)
class BShort : AShort()

open class AInt(vararg val x: Int)
class BInt : AInt()

open class AChar(vararg val x: Char)
class BChar : AChar()

open class ALong(vararg val x: Long)
class BLong : ALong()

open class AFloat(vararg val x: Float)
class BFloat : AFloat()

open class ADouble(vararg val x: Double)
class BDouble : ADouble()

open class AUByte(vararg val x: UByte)
class BUByte : AUByte()

open class AUShort(vararg val x: UShort)
class BUShort : AUShort()

open class AUInt(vararg val x: UInt)
class BUInt : AUInt()

open class AULong(vararg val x: ULong)
class BULong : AULong()

open class ABoolean(vararg val x: Boolean)
class BBoolean : ABoolean()

fun box(): String {
    val a_x_Byte = AByte().x as Any as ByteArray
    if (a_x_Byte.size != 0) return "Fail a ByteArray"

    val b_x_Byte = BByte().x as Any as ByteArray
    if (b_x_Byte.size != 0) return "Fail b ByteArray"

    val a_x_Short = AShort().x as Any as ShortArray
    if (a_x_Short.size != 0) return "Fail a ShortArray"

    val b_x_Short = BShort().x as Any as ShortArray
    if (b_x_Short.size != 0) return "Fail b ShortArray"

    val a_x_Int = AInt().x as Any as IntArray
    if (a_x_Int.size != 0) return "Fail a IntArray"

    val b_x_Int = BInt().x as Any as IntArray
    if (b_x_Int.size != 0) return "Fail b IntArray"

    val a_x_Char = AChar().x as Any as CharArray
    if (a_x_Char.size != 0) return "Fail a CharArray"

    val b_x_Char = BChar().x as Any as CharArray
    if (b_x_Char.size != 0) return "Fail b CharArray"

    val a_x_Long = ALong().x as Any as LongArray
    if (a_x_Long.size != 0) return "Fail a LongArray"

    val b_x_Long = BLong().x as Any as LongArray
    if (b_x_Long.size != 0) return "Fail b LongArray"

    val a_x_Float = AFloat().x as Any as FloatArray
    if (a_x_Float.size != 0) return "Fail a FloatArray"

    val b_x_Float = BFloat().x as Any as FloatArray
    if (b_x_Float.size != 0) return "Fail b FloatArray"

    val a_x_Double = ADouble().x as Any as DoubleArray
    if (a_x_Double.size != 0) return "Fail a DoubleArray"

    val b_x_Double = BDouble().x as Any as DoubleArray
    if (b_x_Double.size != 0) return "Fail b DoubleArray"

    val a_x_UByte = AUByte().x as Any as UByteArray
    if (a_x_UByte.size != 0) return "Fail a UByteArray"

    val b_x_UByte = BUByte().x as Any as UByteArray
    if (b_x_UByte.size != 0) return "Fail b UByteArray"

    val a_x_UShort = AUShort().x as Any as UShortArray
    if (a_x_UShort.size != 0) return "Fail a UShortArray"

    val b_x_UShort = BUShort().x as Any as UShortArray
    if (b_x_UShort.size != 0) return "Fail b UShortArray"

    val a_x_UInt = AUInt().x as Any as UIntArray
    if (a_x_UInt.size != 0) return "Fail a UIntArray"

    val b_x_UInt = BUInt().x as Any as UIntArray
    if (b_x_UInt.size != 0) return "Fail b UIntArray"

    val a_x_ULong = AULong().x as Any as ULongArray
    if (a_x_ULong.size != 0) return "Fail a ULongArray"

    val b_x_ULong = BULong().x as Any as ULongArray
    if (b_x_ULong.size != 0) return "Fail b ULongArray"

    val a_x_Boolean = ABoolean().x as Any as BooleanArray
    if (a_x_Boolean.size != 0) return "Fail a BooleanArray"

    val b_x_Boolean = BBoolean().x as Any as BooleanArray
    if (b_x_Boolean.size != 0) return "Fail b BooleanArray"

    return "OK"
//    return Local().obj.result()
}