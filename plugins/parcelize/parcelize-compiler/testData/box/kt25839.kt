// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class User : Parcelable

@Parcelize
class <!PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY!>User2<!>() : Parcelable

fun box() = parcelTest { parcel ->
    val user = User()
    val user2 = User2()

    user.writeToParcel(parcel, 0)
    user2.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    parcelableCreator<User>().createFromParcel(parcel)
    parcelableCreator<User2>().createFromParcel(parcel)
}