// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Test(val a: List<String>) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(listOf("A", "B"))

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val first2 = readFromParcel<Test>(parcel)

    assert(first == first2)
}