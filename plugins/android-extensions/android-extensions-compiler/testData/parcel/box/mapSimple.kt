// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Test(val a: Map<String, String>) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(mapOf("A" to "B", "C" to "D"))

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    val first2 = readFromParcel<Test>(parcel)

    assert(first == first2)
}