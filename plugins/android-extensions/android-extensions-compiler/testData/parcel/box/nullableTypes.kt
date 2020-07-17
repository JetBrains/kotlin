// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Foo(val a: String) : Parcelable

@Parcelize
data class Test(
    val str1: String,
    val str2: String?,
    val int1: Int,
    val int2: Int?,
    val foo: Foo?
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test("John", "Smith", 20, 30, Foo("a"))
    val second = Test("A", null, 20, null, null)

    first.writeToParcel(parcel, 0)
    second.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val first2 = readFromParcel<Test>(parcel)
    val second2 = readFromParcel<Test>(parcel)

    assert(first == first2)
    assert(second == second2)
}