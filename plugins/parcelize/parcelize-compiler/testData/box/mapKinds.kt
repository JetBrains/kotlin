// WITH_STDLIB
// FULL_JDK

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.util.*
import kotlinx.collections.immutable.*

@Parcelize
data class Test(
        val a: Map<String, String>,
        val b: MutableMap<String, String>,
        val c: HashMap<String, String>,
        val d: LinkedHashMap<String, String>,
        val e: TreeMap<String, String>,
        val f: SortedMap<String, String>,
        val g: NavigableMap<String, String>,
        val h: PersistentMap<String, String>,
        val i: ImmutableMap<String, String>,
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(
            a = mapOf("A" to "B"),
            b = mutableMapOf("A" to "B"),
            c = HashMap<String, String>().apply { put("A", "B") },
            d = LinkedHashMap<String, String>().apply { put("A", "B") },
            e = TreeMap<String, String>().apply { put("A", "B") },
            f = TreeMap<String, String>().apply { put("A", "B") },
            g = TreeMap<String, String>().apply { put("A", "B") },
            h = persistentMapOf("A" to "B"),
            i = persistentMapOf("A" to "B"),
    )

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val first2 = parcelableCreator<Test>().createFromParcel(parcel)

    assert(first == first2)
    assert((first.c as HashMap<*, *>).size == 1)
    assert((first2.e as TreeMap<*, *>).size == 1)
}