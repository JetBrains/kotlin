// WITH_STDLIB
// IGNORE_BACKEND: JVM

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class UnsignedTypes(
    val ub: UByte, val us: UShort, val ui: UInt, val ul: ULong,
    val nub: UByte?, val nus: UShort?, val nui: UInt?, val nul: ULong?
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = UnsignedTypes(
        3.toUByte(), 10.toUShort(), 300.toUInt(), 3000UL,
        3.toUByte(), 10.toUShort(), 300.toUInt(), 3000UL,
    )
    val second = UnsignedTypes(
        UByte.MAX_VALUE, UShort.MAX_VALUE, UInt.MAX_VALUE, ULong.MAX_VALUE,
        UByte.MAX_VALUE, UShort.MAX_VALUE, UInt.MAX_VALUE, ULong.MAX_VALUE,
    )
    val third = UnsignedTypes(
        UByte.MIN_VALUE, UShort.MIN_VALUE, UInt.MIN_VALUE, ULong.MIN_VALUE,
        null, null, null, null,
    )

    first.writeToParcel(parcel, 0)
    second.writeToParcel(parcel, 0)
    third.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val parcelableCreator = parcelableCreator<UnsignedTypes>()
    val first2 = parcelableCreator.createFromParcel(parcel)
    val second2 = parcelableCreator.createFromParcel(parcel)
    val third2 = parcelableCreator.createFromParcel(parcel)

    assert(first == first2)
    assert(second == second2)
    assert(third == third2)
}
