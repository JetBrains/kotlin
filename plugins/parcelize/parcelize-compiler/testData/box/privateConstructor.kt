// IGNORE_BACKEND: JVM
// See KT-24842, https://issuetracker.google.com/189858212
// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class Data private constructor(val value: String) : Parcelable {
    constructor() : this("OK")
}

fun box() = parcelTest { parcel ->
    Data().writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val data = readFromParcel<Data>(parcel)
    assert(data.value == "OK")
}
