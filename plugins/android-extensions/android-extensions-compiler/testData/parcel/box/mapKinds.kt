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
        val a: Map<String, String>,
        val b: MutableMap<String, String>,
        val c: HashMap<String, String>,
        val d: LinkedHashMap<String, String>,
        val e: TreeMap<String, String>
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(
            a = mapOf("A" to "B"),
            b = mutableMapOf("A" to "B"),
            c = HashMap<String, String>().apply { put("A", "B") },
            d = LinkedHashMap<String, String>().apply { put("A", "B") },
            e = TreeMap<String, String>().apply { put("A", "B") }
    )

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    val first2 = readFromParcel<Test>(parcel)

    assert(first == first2)
    assert((first.c as HashMap<*, *>).size == 1)
    assert((first2.e as TreeMap<*, *>).size == 1)
}