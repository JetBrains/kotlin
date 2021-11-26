// IGNORE_BACKEND: JVM
// Fails with a VerifyError in Foo.writeToParcel
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray

@Parcelize
data class PInt(val x: Int) : Parcelable

@Parcelize
data class Foo(val values: SparseArray<SparseArray<Parcelable>>) : Parcelable

fun box() = parcelTest { parcel ->
    val pint = PInt(0)
    val sarray = SparseArray<Parcelable>()
    sarray.put(0, pint)
    val sarray2 = SparseArray<SparseArray<Parcelable>>()
    sarray2.put(1, sarray)
    val foo = Foo(sarray2)

    foo.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val foo2 = parcelableCreator<Foo>().createFromParcel(parcel)
    assert(foo2.values.size() == 1)
    assert(foo2.values.get(1) != null) // SparseArray.contains was only added in Android R
    assert(foo2.values.get(1).size() == 1)
    assert(foo2.values.get(1).get(0) != null)
    assert(foo2.values.get(1).get(0) == pint)
}
