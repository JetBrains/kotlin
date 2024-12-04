// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
open class Base(open var s: String): Parcelable

@Parcelize
data class Derived(override var s: String): Base(s) {
    fun getSuper() = super.s
    fun setSuper(x: String) {
        super.s = x
    }
}

fun box() = parcelTest { parcel ->
    val expected = Derived("test")
    expected.s = "test1"
    expected.setSuper("test2")
    expected.writeToParcel(parcel, 0)
    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)
    val got = parcelableCreator<Derived>().createFromParcel(parcel)
    assert(expected == got) { "expected $expected != got $got "}
    assert(expected.getSuper() == got.getSuper()) { "expected ${expected.getSuper()} != got ${got.getSuper()}"}
}
