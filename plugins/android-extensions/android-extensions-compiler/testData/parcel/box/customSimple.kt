// IGNORE_BACKEND: JVM
// See KT-38105
// Throws IllegalAccessError, since the code tries to access the private companion field directly from the generated User$Creator class.
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class User(val firstName: String, val secondName: String, val age: Int) : Parcelable {
    private companion object : Parceler<User> {
        override fun User.write(parcel: Parcel, flags: Int) {
            parcel.writeString(firstName)
            parcel.writeString(secondName)
        }

        override fun create(parcel: Parcel) = User(parcel.readString(), parcel.readString(), 0)
    }
}

fun box() = parcelTest { parcel ->
    val user = User("John", "Smith", 20)
    user.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val user2 = readFromParcel<User>(parcel)

    assert(user.firstName == user2.firstName)
    assert(user.secondName == user2.secondName)
    assert(user2.age == 0)
}
