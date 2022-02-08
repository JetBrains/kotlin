// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

annotation class SerializableLike

@Suppress("DEPRECATED_ANNOTATION")
@Parcelize @SerializableLike
data class User(val firstName: String, val secondName: String, val age: Int) : Parcelable

fun box() = parcelTest { parcel ->
    val user = User("John", "Smith", 20)
    user.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    @Suppress("UNCHECKED_CAST")
    val creator = User::class.java.getDeclaredField("CREATOR").get(null) as Parcelable.Creator<User>
    val user2 = creator.createFromParcel(parcel)
    assert(user == user2)
}