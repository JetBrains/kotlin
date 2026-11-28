// https://issuetracker.google.com/590764741
// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import android.os.Parcelable
import kotlinx.parcelize.*

data class Box<T : List<Int>>(val x: T)

@Parcelize
data class Holder(
    val inProjection: @DataClass Box<in ArrayList<Int>>,
    val starProjection: @DataClass Box<*>,
) : Parcelable

fun box() = parcelTest { parcel ->
    val holder = Holder(Box<List<Int>>(listOf(1, 2)), Box(listOf(3)))
    holder.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val holder2 = parcelableCreator<Holder>().createFromParcel(parcel)
    check(holder == holder2) { "Expected $holder, got $holder2" }
    check(parcel.dataAvail() == 0) { "Parcel has ${parcel.dataAvail()} unread bytes" }
}
