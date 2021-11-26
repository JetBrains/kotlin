// See https://issuetracker.google.com/174827004
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class Section<T : Parcelable>(val title: String, val faTitle: T) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Section<Bundle>("title", Bundle())
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Section<Bundle>>().createFromParcel(parcel)

    assert(test.title == test2.title)
    assert(test2.faTitle.size() == 0)
}
