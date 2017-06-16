// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@MagicParcel
data class Test(
        val a: Map<String, String>,
        val b: Map<String?, String>,
        val c: Map<String, String?>,
        val d: Map<String, Map<Int, String>>,
        val e: Map<Int?, List<String>>,
        val f: Map<Boolean, Boolean>,
        val g: Map<String, Map<String, Map<String, String>>>
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(
            a = mapOf("A" to "B", "C" to "D"),
            b = mapOf("A" to "B", null to "D", "E" to "F"),
            c = mapOf("A" to null, "C" to "D"),
            d = mapOf("A" to mapOf(1 to "", 2 to "x")),
            e = mapOf(1 to listOf("", ""), null to listOf()),
            f = mapOf(true to false, false to true),
            g = mapOf("A" to mapOf("B" to mapOf("C" to "D", "E" to "F")))
    )

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    val first2 = readFromParcel<Test>(parcel)

    assert(first == first2)
}