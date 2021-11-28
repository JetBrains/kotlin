// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

// Starts with A, should be loaded before other classes
abstract class AParcelable : Parcelable

@Parcelize
data class P1(val a: String) : AParcelable()

sealed class Sealed : AParcelable()

@Parcelize
data class Sealed1(val a: Int) : Sealed()

@Parcelize
data class Test(val a: P1, val b: AParcelable, val c: Sealed, val d: Sealed1) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test(P1(""), P1("My"), Sealed1(1), Sealed1(5))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Test>().createFromParcel(parcel)
    assert(test == test2)
}