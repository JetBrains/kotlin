// IGNORE_BACKEND: JVM
// StackOverflowError caused by infinite loop in MyObject.writeToParcel
// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
private object MyObject : Parcelable

fun box() = parcelTest { parcel ->
    MyObject.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    readFromParcel<MyObject>(parcel)
}
