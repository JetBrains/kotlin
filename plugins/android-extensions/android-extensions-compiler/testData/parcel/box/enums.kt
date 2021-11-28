// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

enum class Color {
    BLACK, WHITE
}

@Parcelize
data class Test(val name: String, val color: Color) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test("John", Color.WHITE)
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<Test>(parcel)
    assert(test == test2)
}