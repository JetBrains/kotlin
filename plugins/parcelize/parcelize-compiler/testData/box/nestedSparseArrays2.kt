// IGNORE_BACKEND: JVM
// See https://issuetracker.google.com/177856512
// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray

@Parcelize
class Data(val values: SparseArray<SparseArray<Parcelable>>) : Parcelable

fun box() = parcelTest { parcel ->
    val innerArray = SparseArray<Parcelable>()
    innerArray.append(20, Bundle())
    var array = SparseArray<SparseArray<Parcelable>>()
    array.append(10, innerArray)
    val first = Data(array)

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val second = readFromParcel<Data>(parcel)
    assert(second.values.size() == 1)
    val secondInnerArray = second.values.get(10)
    assert(secondInnerArray.size() == 1)
    val innerBundle = secondInnerArray.get(20)
    assert(innerBundle is Bundle)
    assert((innerBundle as Bundle).size() == 0)
}
