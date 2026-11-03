// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable

interface Base : Parcelable

@Parcelize
data class User(val name: String) : Base

fun box() = parcelTest { parcel ->
    val user = User("John")
    val base: Base = user
    base.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val user2 = parcelableCreator<User>().createFromParcel(parcel)
    assert(user == user2)
}
