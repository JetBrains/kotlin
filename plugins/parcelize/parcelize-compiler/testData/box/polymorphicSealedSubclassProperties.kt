// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
@PolymorphicSealed
sealed class Foo : Parcelable {
    data class A(val x: Int) : Foo()
    object B : Foo()
    object Inner : Foo()
    data class C(val x: String) : Foo()
}

@Parcelize
data class Bar(val a: Foo.A, val b: Foo.B, val c: Foo.C, val foo: Foo) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Bar(Foo.A(1024), Foo.B, Foo.C("OK"), Foo.A(1))

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val second = parcelableCreator<Bar>().createFromParcel(parcel)

    assert(first == second)
}

