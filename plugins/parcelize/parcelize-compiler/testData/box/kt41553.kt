// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

@Parcelize
data class A(val params: @RawValue Array<*>? = null): Parcelable

fun box() = parcelTest { parcel ->
    val a1 = A(arrayOf<Int>(1,2,3))
    val b1 = A()
    a1.writeToParcel(parcel, 0)
    b1.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val parcelableCreator = parcelableCreator<A>()
    val a2 = parcelableCreator.createFromParcel(parcel)
    assert(a2.params != null)
    assert(Arrays.equals(a1.params!!, a2.params!!))
    val b2 = parcelableCreator.createFromParcel(parcel)
    assert(b1 == b2)
}
