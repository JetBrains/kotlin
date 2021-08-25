// IGNORE_BACKEND: JVM
// See https://issuetracker.google.com/178394826
// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

fun box() = parcelTest { parcel ->
    parcel.writeParcelable(MyObject, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    parcel.readParcelable<MyObject>(null)
}

@Parcelize
private object MyObject : Parcelable
