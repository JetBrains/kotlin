// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class User : Parcelable

@Parcelize
class User2() : Parcelable

fun box() = parcelTest { parcel ->
    val user = User()
    val user2 = User2()

    user.writeToParcel(parcel, 0)
    user2.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)

    readFromParcel<User>(parcel)
    readFromParcel<User2>(parcel)
}