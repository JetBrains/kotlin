// WITH_STDLIB

@file:JvmName("TestKt")

package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import kotlinx.collections.immutable.*

@Parcelize
data class Test(val a: PersistentMap<String, String>) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(persistentMapOf("A" to "B", "C" to "D"))

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val first2 = parcelableCreator<Test>().createFromParcel(parcel)

    assert(first == first2)
}