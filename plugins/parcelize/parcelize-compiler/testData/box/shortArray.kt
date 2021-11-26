// WITH_STDLIB
// IGNORE_BACKEND: JVM

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class A(val value: ShortArray) : Parcelable

fun box() = parcelTest { parcel ->
    val a = A(shortArrayOf(0, 1, 2, 3))
    a.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val a2 = parcelableCreator<A>().createFromParcel(parcel)
    assert(a.value.contentEquals(a2.value))
}
