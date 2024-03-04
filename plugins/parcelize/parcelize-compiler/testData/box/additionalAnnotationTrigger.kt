// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import android.os.Parcelable
import kotlinx.parcelize.*

annotation class TriggerParcelize

@TriggerParcelize
data class User(val name: String) : Parcelable

fun box() = parcelTest { parcel ->
    val user = User("John")
    user.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val user2 = parcelableCreator<User>().createFromParcel(parcel)
    assert(user == user2)
}