// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
sealed class Foo : Parcelable {
    data class A(val x: Int) : Foo()
    object B : Foo()
    sealed class Inner : Foo()
}

data class C(val x: String) : Foo()

@Parcelize
data class Bar(val a: Foo.A, val b: Foo.B, val c: C, val foo: Foo) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Bar(Foo.A(1024), Foo.B, C("OK"), Foo.A(1))

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val second = parcelableCreator<Bar>().createFromParcel(parcel)

    assert(first == second)
}
