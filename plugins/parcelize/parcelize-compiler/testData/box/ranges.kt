// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import kotlin.ranges.*

@Parcelize
data class Ranges(
    val intRange: IntRange,
    val charRange: CharRange,
    val longRange: LongRange
) : Parcelable

fun box() = parcelTest { parcel ->
    val expected = Ranges(2..10, 'a'..'z', 1L..10000L)

    expected.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val parcelableCreator = parcelableCreator<Ranges>()
    val got = parcelableCreator.createFromParcel(parcel)

    assert(expected == got) { "not what we wanted EXECTED $expected but GOT $got"}
}
