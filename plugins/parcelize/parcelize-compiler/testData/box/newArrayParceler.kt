// WITH_STDLIB
// IGNORE_BACKEND: JVM

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

abstract class UserParceler : Parceler<User> {
    override fun User.write(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
    }

    override fun newArray(size: Int): Array<User> {
        return Array(size + 1) { User(null) }
    }
}

@Parcelize
class User(val name: String?) : Parcelable {
    companion object : UserParceler() {
        override fun create(parcel: Parcel) = User(parcel.readString())
    }
}

fun box() = parcelTest { parcel ->
    val user = User("John")
    val user2 = User("Joe")
    val array = arrayOf(user, user2)
    parcel.writeTypedArray(array, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val creator = parcelableCreator<User>()
    val result = parcel.createTypedArray(creator)

    assert(result.size == 3)
    assert(result[0].name == user.name)
    assert(result[1].name == user2.name)
    assert(result[2].name == null)
}
