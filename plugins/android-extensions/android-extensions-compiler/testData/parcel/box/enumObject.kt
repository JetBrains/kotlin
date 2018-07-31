// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
enum class Color : Parcelable { BLACK, WHITE }

@Parcelize
object Obj : Parcelable

fun box() = parcelTest { parcel ->
    val black = Color.BLACK
    val obj = Obj

    black.writeToParcel(parcel, 0)
    obj.writeToParcel(parcel, 0)

    println(black)
    println(obj)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    val black2 = readFromParcel<Color>(parcel)
    val obj2 = readFromParcel<Obj>(parcel)

    println(black2)
    println(obj2)

    assert(black2 == black)
    assert(obj2 != null)
}