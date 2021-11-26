// !OPT_IN: kotlin.ExperimentalUnsignedTypes
// WITH_STDLIB
// IGNORE_BACKEND: JVM

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.util.Arrays

@Parcelize
data class Test(
    val a: UByteArray,
    val b: UShortArray,
    val c: UIntArray,
    val d: ULongArray,
    val e: UByteArray?,
    val f: UShortArray?,
    val g: UIntArray?,
    val h: ULongArray?
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Test) return false

        if (!a.contentEquals(other.a)) return false
        if (!b.contentEquals(other.b)) return false
        if (!c.contentEquals(other.c)) return false
        if (!d.contentEquals(other.d)) return false
        if (!e.contentEquals(other.e)) return false
        if (!f.contentEquals(other.f)) return false
        if (!g.contentEquals(other.g)) return false
        if (!h.contentEquals(other.h)) return false

        return true
    }

    override fun hashCode(): Int {
        return 0
    }
}

fun box() = parcelTest { parcel ->
    val first = Test(
        a = ubyteArrayOf(1U, 2U, 3U),
        b = ushortArrayOf(2U, 3U, 4U),
        c = uintArrayOf(3U, 4U, 5U),
        d = ulongArrayOf(4U, 5U, 6U),
        e = ubyteArrayOf(UByte.MAX_VALUE, UByte.MIN_VALUE),
        f = ushortArrayOf(UShort.MAX_VALUE, UShort.MIN_VALUE),
        g = uintArrayOf(UInt.MAX_VALUE, UInt.MIN_VALUE),
        h = ulongArrayOf(ULong.MAX_VALUE, ULong.MIN_VALUE),
    )
    val second = Test(
        a = ubyteArrayOf(),
        b = ushortArrayOf(),
        c = uintArrayOf(),
        d = ulongArrayOf(),
        e = null,
        f = null,
        g = null,
        h = null,
    )

    first.writeToParcel(parcel, 0)
    second.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val creator = parcelableCreator<Test>()
    val first2 = creator.createFromParcel(parcel)
    val second2 = creator.createFromParcel(parcel)

    assert(first == first2)
    assert(second == second2)
}