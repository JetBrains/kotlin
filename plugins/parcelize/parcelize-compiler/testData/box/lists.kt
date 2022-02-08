// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Test(
        val a: List<String>,
        val b: List<String?>,
        val c: List<Int>,
        val d: List<Int?>,
        val e: List<List<String>?>,
        val f: List<List<List<Int>>>
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test(
            a = listOf("A", "B"),
            b = listOf("A", null, "C"),
            c = listOf(1, 2, 3),
            d = listOf(1, null, 5),
            e = listOf(listOf("A", "B"), listOf(), null),
            f = listOf(listOf(listOf(1, 2), listOf(3)), listOf(listOf(5, 3)))
    )

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val first2 = parcelableCreator<Test>().createFromParcel(parcel)

    assert(first == first2)

    assert(first2.a == listOf("A", "B"))
    assert(first2.b.size == 3)
}