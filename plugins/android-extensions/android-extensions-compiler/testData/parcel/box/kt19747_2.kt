// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

interface IJHelp {
    val j1: String
}

class JHelp(override var j1: String): IJHelp, Serializable {
    val j2 = 9
}

@Parcelize
class J(val j: @RawValue JHelp) : Parcelable

fun box() = parcelTest { parcel ->
    val test = J(JHelp("A"))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<J>(parcel)

    assert(test.j.j1 == test2.j.j1)
}