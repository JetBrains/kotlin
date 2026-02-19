// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

data class Color(val r: Int, val g: Int, val b: Int)

data class Box<T>(internal val x: T)

@Parcelize
data class Test(val name: @DataClass Box<String>, val color: @DataClass Color) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test(Box("John"), Color(0, 255, 0))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Test>().createFromParcel(parcel)
    assert(test == test2)
}