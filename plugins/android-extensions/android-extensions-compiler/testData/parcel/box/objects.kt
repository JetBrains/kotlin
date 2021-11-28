// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

object Obj1 {
    object Obj2
}

@Parcelize
data class Test(val o1: Obj1, val o2: Obj1.Obj2, val com: Com) : Parcelable {
    companion object Com {

    }
}

fun box() = parcelTest { parcel ->
    val test = Test(Obj1, Obj1.Obj2, Test.Com)
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<Test>(parcel)
    assert(test == test2)
}