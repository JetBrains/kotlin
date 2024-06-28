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

typealias Parceler2 = Parceler1

object Parceler3 : Parceler<String> {
    override fun create(parcel: Parcel) = parcel.readString().uppercase()

    override fun String.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this)
    }
}

@Parcelize
@TypeParceler<String, Parceler2>
data class Test(
    val a: String,
    @<!REDUNDANT_TYPE_PARCELER!>TypeParceler<!><String, Parceler1> val b: String,
    @TypeParceler<String, Parceler3> val c: CharSequence,
    val d: @WriteWith<Parceler3> String,
) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test("Abc", "Abc", "Abc", "Abc")
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Test>().createFromParcel(parcel)

    assert(test.a == "Abc" && test.b == "Abc" && test.c == "Abc" && test.d == "Abc")
    assert(test2.a == "3" && test2.b == "3" && test2.c == "Abc" && test2.d == "ABC")
}
