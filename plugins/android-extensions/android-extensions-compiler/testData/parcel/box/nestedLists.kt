// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

@Parcelize
class Data(val data: List<Array<Int>>) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Data(listOf(arrayOf(0, 1)))

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val second = readFromParcel<Data>(parcel)
    assert(second.data.size == 1)
    assert(Arrays.equals(second.data[0], arrayOf(0, 1)))
}
