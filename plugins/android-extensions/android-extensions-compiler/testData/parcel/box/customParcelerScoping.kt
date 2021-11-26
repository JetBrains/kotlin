// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseBooleanArray

object Parceler1 : Parceler<Int> {
    override fun create(parcel: Parcel) = -parcel.readInt()

    override fun Int.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(this)
    }
}

object Parceler2 : Parceler<Int> {
    override fun create(parcel: Parcel) = parcel.readString().length

    override fun Int.write(parcel: Parcel, flags: Int) {
        parcel.writeString("Abc")
    }
}

@Parcelize
@TypeParceler<Int, Parceler1>
data class Ints(val a: Int, @TypeParceler<Int, Parceler2> val b: Int, val c: @WriteWith<Parceler2> Int) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Ints(1, 1, 1)
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<Ints>(parcel)

    assert(test2.a == -test.a)
    assert(test2.b == -test.b)
    assert(test2.c == 3)
}
