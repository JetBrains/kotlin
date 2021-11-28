// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class User(val firstName: String, val secondName: String, val age: Int) : Parcelable

fun box() = parcelTest { parcel ->
    val user = User("John", "Smith", 20)
    val user2 = User("Joe", "Bloggs", 30)
    val array = arrayOf(user, user2)
    parcel.writeTypedArray(array, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val creator = parcelableCreator<User>()
    val result = parcel.createTypedArray(creator)

    assert(result.size == 2)
    assert(result[0].firstName == user.firstName)
    assert(result[1].firstName == user2.firstName)
}