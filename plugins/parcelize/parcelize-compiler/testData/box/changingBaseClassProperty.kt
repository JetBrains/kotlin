// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class A(var i1: Int, var i2: Int): Parcelable

@Parcelize
open class Base(var s1: String, var a: A): Parcelable

@Parcelize
open class Derived(var s2: String, s1: String, a: A): Base(s1, a)

@Parcelize
class DerivedEvenMore(
    var a2: A,
): Derived("a", "b", A(1, 1))

fun box() = parcelTest { parcel ->
    val expected = Derived("hello", "whoisit", A(1, 2))
    val expected2 = DerivedEvenMore(A(2, 2))

    expected.s1 = "world"
    expected.a.i2 = 123
    expected.writeToParcel(parcel, 0)

    expected2.s1 = "world"
    expected2.a.i2 = 123
    expected2.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val got = parcelableCreator<Derived>().createFromParcel(parcel)
    val got2 = parcelableCreator<DerivedEvenMore>().createFromParcel(parcel)

    assert(expected.a == got.a) { "expected.a != got.a => ${expected.a} != ${got.a}" }
    assert(expected.s2 == got.s2) { "expected.a != got.a => ${expected.s2} != ${got.s2}" }
    assert(expected.s1 == got.s1) { "expected.s1 != got.s1 => ${expected.s1} != ${got.s1}" }

    assert(expected2.a == got2.a) { "expected2.a != got2.a => ${expected2.a} != ${got2.a}" }
    assert(expected2.s2 == got2.s2) { "expected2.a != got2.a => ${expected2.s2} != ${got2.s2}" }
    assert(expected2.s1 == got2.s1) { "expected2.s1 != got2.s1 => ${expected2.s1} != ${got2.s1}" }
    assert(expected2.a2 == got2.a2) { "expected2.a2 != got2.a2 => ${expected2.a2} != ${got2.a2}" }
}