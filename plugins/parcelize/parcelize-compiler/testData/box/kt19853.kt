// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class A(val value: Int) : Parcelable

fun box() = parcelTest { parcel ->
    parcel.writeTypedList(listOf(A(0), A(1)))

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val creator = parcelableCreator<A>()
    val deserialized = mutableListOf<A>()
    parcel.readTypedList(deserialized, creator)
    assert(deserialized.size == 2)
    assert(deserialized[0].value == 0)
    assert(deserialized[1].value == 1)
}
