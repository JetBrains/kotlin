// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

@Parcelize
class Data(val data: Map<Array<Int>, Array<Int>>) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Data(mapOf(arrayOf(0) to arrayOf(1)))

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val second = parcelableCreator<Data>().createFromParcel(parcel)
    assert(second.data.size == 1)
    val entry = second.data.entries.single()
    assert(Arrays.equals(entry.key, arrayOf(0)))
    assert(Arrays.equals(entry.value, arrayOf(1)))
}