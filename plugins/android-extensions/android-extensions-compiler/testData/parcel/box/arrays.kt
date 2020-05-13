// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

@Parcelize
data class Test(
        val a: Array<String>,
        val b: Array<String?>,
        val c: IntArray,
        val d: CharArray?,
        val e: Array<IntArray>,
        val f: Array<List<String>>,
        val g: List<Array<String>>,
        val h: Array<String>?
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Test

        if (!Arrays.equals(a, other.a)) return false
        if (!Arrays.equals(b, other.b)) return false
        if (!Arrays.equals(c, other.c)) return false
        if (!Arrays.equals(d, other.d)) return false
        if (!Arrays.deepEquals(e, other.e)) return false
        if (!Arrays.equals(f, other.f)) return false

        if (g.size != other.g.size) return false
        if (!g.zip(other.g).all { (f, s) -> Arrays.equals(f, s) }) return false

        if (!Arrays.equals(h, other.h)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(a)
        result = 31 * result + Arrays.hashCode(b)
        result = 31 * result + Arrays.hashCode(c)
        result = 31 * result + (d?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + Arrays.hashCode(e)
        result = 31 * result + Arrays.hashCode(f)
        result = 31 * result + g.hashCode()
        result = 31 * result + (h?.let { Arrays.hashCode(it) } ?: 0)
        return result
    }
}

fun box() = parcelTest { parcel ->
    val first = Test(
            a = arrayOf("A", "B", "C"),
            b = arrayOf("A", null, "B"),
            c = intArrayOf(1, 2, 3),
            d = null,
            e = arrayOf(intArrayOf(2, 4, 1), intArrayOf(10, 20)),
            f = arrayOf(listOf("A"), listOf("B", "C")),
            g = listOf(arrayOf("Z", "X"), arrayOf()),
            h = arrayOf("")
    )

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val first2 = readFromParcel<Test>(parcel)

    assert(first == first2)
}