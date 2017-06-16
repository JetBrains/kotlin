// WITH_RUNTIME
// FULL_JDK

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.util.*

@MagicParcel
data class Test(
        val a: List<String>,
        val b: MutableList<String>,
        val c: ArrayList<String>,
        val d: LinkedList<String>,
        val e: Set<String>,
        val f: MutableSet<String>,
        val g: TreeSet<String>,
        val h: HashSet<String>,
        val i: LinkedHashSet<String>
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(
            a = listOf("A"),
            b = mutableListOf("B"),
            c = ArrayList<String>().apply { this += "C" },
            d = LinkedList<String>().apply { this += "D" },
            e = setOf("E"),
            f = mutableSetOf("F"),
            g = TreeSet<String>().apply { this += "G" },
            h = HashSet<String>().apply { this += "H" },
            i = LinkedHashSet<String>().apply { this += "I" }
    )

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    val first2 = readFromParcel<Test>(parcel)

    assert(first == first2)
    assert((first.d as LinkedList<*>).size == 1)
    assert((first2.h as HashSet<*>).size == 1)
}