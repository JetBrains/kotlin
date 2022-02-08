// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Foo(val value: Int) : Parcelable

@Parcelize
data class Box<T : Parcelable>(val box: T) : Parcelable

fun box() = parcelTest { parcel ->
    val foo = Foo(42)
    val box = Box(foo)
    box.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val boxLoaded = parcelableCreator<Box<Foo>>().createFromParcel(parcel)
    assert(box == boxLoaded)
}