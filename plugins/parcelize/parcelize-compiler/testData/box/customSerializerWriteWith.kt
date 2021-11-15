// IGNORE_BACKEND: JVM
// See KT-38107
// The JVM backend is missing support for custom parcelers in List<String>
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

object Parceler1 : Parceler<String> {
    override fun create(parcel: Parcel) = parcel.readInt().toString()

    override fun String.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(length)
    }
}

object Parceler2 : Parceler<List<String>> {
    override fun create(parcel: Parcel) = listOf(parcel.readString())

    override fun List<String>.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this.joinToString(","))
    }
}

@Parcelize
data class Test(
        val a: String,
        val b: @WriteWith<Parceler1> String,
        val c: List<@WriteWith<Parceler1> String>,
        val d: @WriteWith<Parceler2> List<String>,
        val e: @WriteWith<Parceler2> List<@WriteWith<Parceler1> String>
) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test("Abc", "Abc", listOf("A", "bc"), listOf("A", "bc"), listOf("A", "bc"))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Test>().createFromParcel(parcel)

    with (test) {
        assert(a == "Abc" && b == "Abc" && c == listOf("A", "bc") && d == listOf("A", "bc") && e == listOf("A", "bc"))
    }

    with (test2) {
        assert(a == "Abc" && b == "3" && c == listOf("1", "2") && d == listOf("A,bc") && e == listOf("A,bc"))
    }
}
