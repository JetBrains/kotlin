// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

fun box() = doTest { creator ->
    assert(creator.newArray(5) != null)
}

fun doTest(work: (Parcelable.Creator<DummyParcelable>) -> Unit): String {
    val dummy = DummyParcelable(42)

    val clazz = dummy.javaClass
    val field = clazz.getDeclaredField("CREATOR")
    @Suppress("UNCHECKED_CAST")
    val creator = field.get(dummy) as Parcelable.Creator<DummyParcelable>

    val parcel = Parcel.obtain()
    dummy.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)

    work(creator)
    return "OK"
}

@Parcelize
data class DummyParcelable(val int: Int): Parcelable