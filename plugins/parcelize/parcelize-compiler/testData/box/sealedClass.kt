// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

sealed class Foo : Parcelable {
    @Parcelize
    data class A(val x: Int) : Foo()

    @Parcelize
    data class B (val x: String) : Foo()
}

@Parcelize
data class Bar(val a: Foo) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Bar(Foo.B("OK"))

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val second = parcelableCreator<Bar>().createFromParcel(parcel)

    assert(first == second)
}