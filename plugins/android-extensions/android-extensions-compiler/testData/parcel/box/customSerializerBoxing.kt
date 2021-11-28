// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

object Parceler1 : Parceler<Int> {
    override fun create(parcel: Parcel) = -parcel.readInt()

    override fun Int.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(this)
    }
}

object Parceler2 : Parceler<Long> {
    override fun create(parcel: Parcel) = parcel.readString().length.toLong()

    override fun Long.write(parcel: Parcel, flags: Int) {
        parcel.writeString("Abc")
    }
}

@Parcelize
data class Test(
        val a: Int,
        @TypeParceler<Int, Parceler1> val b: Int,
        @TypeParceler<Long, Parceler2> val c: Long,
        @TypeParceler<Int, Parceler1> val d: List<Int>,
        @TypeParceler<Long, Parceler2> val e: LongArray
) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test(5, 5, 50L, listOf(1, 2, 3), longArrayOf(3, 2, 1))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<Test>(parcel)

    println(test.toString())
    println(test2.toString())

    with (test) {
        assert(a == 5 && b == 5 && c == 50L && d == listOf(1, 2, 3) && Arrays.equals(e, longArrayOf(3, 2, 1)))
    }

    with (test2) {
        assert(a == 5 && b == -5 && c == 3L && d == listOf(-1, -2, -3) && Arrays.equals(e, longArrayOf(3, 3, 3)))
    }
}