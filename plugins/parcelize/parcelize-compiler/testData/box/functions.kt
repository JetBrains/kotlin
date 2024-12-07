// WITH_STDLIB
// LAMBDAS: CLASS

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Test(val callback: () -> Int = { 0 }, val suspendCallback: suspend () -> Int = { 0 }) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test({ 1 }, { 1 })
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    parcelableCreator<Test>().createFromParcel(parcel)

    assert(test.callback() == 1)
}
