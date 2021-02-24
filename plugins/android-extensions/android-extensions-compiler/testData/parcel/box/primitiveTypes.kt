// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class PrimitiveTypes(
        val boo: Boolean,
        val c: Char,
        val byt: Byte,
        val s: Short,
        val i: Int,
        val f: Float,
        val l: Long,
        val d: Double
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = PrimitiveTypes(true, '#', 3.toByte(), 10.toShort(), -300, -5.0f, Long.MAX_VALUE, 3.14)
    val second = PrimitiveTypes(false, '\n', Byte.MIN_VALUE, Short.MIN_VALUE, Int.MIN_VALUE, Float.POSITIVE_INFINITY,
                                Long.MAX_VALUE, Double.NEGATIVE_INFINITY)

    first.writeToParcel(parcel, 0)
    second.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val first2 = readFromParcel<PrimitiveTypes>(parcel)
    val second2 = readFromParcel<PrimitiveTypes>(parcel)

    assert(first == first2)
    assert(second == second2)
}