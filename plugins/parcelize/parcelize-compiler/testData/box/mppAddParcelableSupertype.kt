// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

package test

import kotlinx.parcelize.*

@AddParcelableSupertype
@Parcelize
data class User(val name: String, val age: Int)

// MODULE: m2-jvm()()(m1-common)
// FILE: android.kt

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable

fun box() = parcelTest { parcel ->
    val user = User("John", 20)
    val parcelable: Parcelable = user
    parcelable.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val user2 = parcelableCreator<User>().createFromParcel(parcel)
    assert(user == user2)
}
