// WITH_STDLIB

@file:JvmName("TestKt")
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class PrimitiveTypes(
    val boo: Boolean, val c: Char, val byt: Byte, val s: Short,
    val i: Int, val f: Float, val l: Long, val d: Double,

    val nboo: Boolean?, val nc: Char?, val nbyt: Byte?, val ns: Short?,
    val ni: Int?, val nf: Float?, val nl: Long?, val nd: Double?,

    val jboo: java.lang.Boolean, val jc: java.lang.Character, val jbyt: java.lang.Byte, val js: java.lang.Short,
    val ji: java.lang.Integer, val jf: java.lang.Float, val jl: java.lang.Long, val jd: java.lang.Double,

    val njboo: java.lang.Boolean?, val njc: java.lang.Character?, val njbyt: java.lang.Byte?, val njs: java.lang.Short?,
    val nji: java.lang.Integer?, val njf: java.lang.Float?, val njl: java.lang.Long?, val njd: java.lang.Double?
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = PrimitiveTypes(
        true, '#', 3.toByte(), 10.toShort(), -300, -5.0f, Long.MAX_VALUE, 3.14,
        true, '#', 3.toByte(), 10.toShort(), -300, -5.0f, Long.MAX_VALUE, 3.14,
        true as java.lang.Boolean, '#' as java.lang.Character,
        3.toByte() as java.lang.Byte, 10.toShort() as java.lang.Short,
        -300 as java.lang.Integer, -5.0f as java.lang.Float,
        10L as java.lang.Long, 3.14 as java.lang.Double,
        true as java.lang.Boolean, '#' as java.lang.Character,
        3.toByte() as java.lang.Byte, 10.toShort() as java.lang.Short,
        -300 as java.lang.Integer, -5.0f as java.lang.Float,
        10L as java.lang.Long, 3.14 as java.lang.Double
    )
    val second = PrimitiveTypes(
        false, '\n', Byte.MIN_VALUE, Short.MIN_VALUE,
        Int.MIN_VALUE, Float.POSITIVE_INFINITY, Long.MAX_VALUE, Double.NEGATIVE_INFINITY,
        null, null, null, null, null, null, null, null,
        false as java.lang.Boolean, '\n' as java.lang.Character,
        Byte.MIN_VALUE as java.lang.Byte, Short.MIN_VALUE as java.lang.Short,
        Int.MIN_VALUE as java.lang.Integer, Float.POSITIVE_INFINITY as java.lang.Float,
        java.lang.Long(Long.MAX_VALUE), java.lang.Double(Double.NEGATIVE_INFINITY),
        null, null, null, null, null, null, null, null
    )

    first.writeToParcel(parcel, 0)
    second.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val parcelableCreator = parcelableCreator<PrimitiveTypes>()
    val first2 = parcelableCreator.createFromParcel(parcel)
    val second2 = parcelableCreator.createFromParcel(parcel)

    assert(first == first2)
    assert(second == second2)
}