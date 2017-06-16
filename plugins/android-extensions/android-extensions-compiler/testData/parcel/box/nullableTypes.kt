// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@MagicParcel
data class Test(
    val str1: String,
    val str2: String?,
    val int1: Int,
    val int2: Int?
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test("John", "Smith", 20, 30)
    val second = Test("A", null, 20, null)

    first.writeToParcel(parcel, 0)
    second.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    val first2 = readFromParcel<Test>(parcel)
    val second2 = readFromParcel<Test>(parcel)

    assert(first == first2)
    assert(second == second2)
}