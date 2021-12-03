// WITH_STDLIB
// IGNORE_BACKEND: JVM

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

class T(val value: Int)

@Parcelize
class A(
    val x: String,
    @IgnoredOnParcel
    val i1: String = "default",
    @IgnoredOnParcel
    val i2: T = T(10),
    val d: String = "default",
) : Parcelable {
    @IgnoredOnParcel
    val a: String = x
}

@Parcelize
object B : Parcelable {
    @IgnoredOnParcel
    val x: T = T(2)
}

fun box() = parcelTest { parcel ->
    val a1 = A("X", "i1", T(0), "d")

    a1.writeToParcel(parcel, 0)
    B.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val a2 = parcelableCreator<A>().createFromParcel(parcel)
    val b = parcelableCreator<B>().createFromParcel(parcel)

    assert(a1.x == a2.x)
    assert(a2.i1 == "default")
    assert(a2.i2.value == 10)
    assert(a1.d == a2.d)
    assert(a1.a == a2.a)
    assert(b == B)
}
