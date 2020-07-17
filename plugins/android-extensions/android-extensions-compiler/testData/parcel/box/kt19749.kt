// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class MHelp(var m1: String): Serializable {
    val m2 = 9
}

@Parcelize
class M(val m: @RawValue MHelp) : Parcelable

fun box() = parcelTest { parcel ->
    val test = M(MHelp("A"))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<M>(parcel)

    assert(test.m.m1 == test2.m.m1)
}