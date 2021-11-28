// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
sealed interface I : Parcelable

@Parcelize
sealed class A : Parcelable

abstract class B
interface J

data class AI(val x: String) : A(), I
class I1 : J, I {
    override fun equals(other: Any?): Boolean {
        return other is I1
    }
}
data class I2(val x: Float) : B(), I

object A1 : A()
open class A2(val x: Int) : A() {
    override fun equals(other: Any?): Boolean {
        return other is A2 && other::class == A2::class && x == other.x
    }
}

@Parcelize
class A3 : A2(3) {
    override fun equals(other: Any?): Boolean {
        return other is A3 && x == other.x
    }
}

@Parcelize
data class C(
    val a: A,
    val i: I,
    val a1: A1,
    val a2: A2,
    val a3: A3,
    val ai: AI,
    val i1: I1,
    val i2: I2,
) : Parcelable

fun box() = parcelTest { parcel ->
    val first = C(
        AI("0"),
        AI("1"),
        A1,
        A2(2),
        A3(),
        AI("4"),
        I1(),
        I2(5.0f),
    )

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val second = parcelableCreator<C>().createFromParcel(parcel)

    assert(first == second)
}
