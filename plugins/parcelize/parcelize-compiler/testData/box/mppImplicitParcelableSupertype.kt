// IGNORE_BACKEND_K1: ANY
// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

package test

import kotlinx.parcelize.*

@Parcelize
data class User(val name: String, val age: Int)

@Parcelize
sealed interface Shape

@Parcelize
data class Circle(val radius: Int) : Shape

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

    val circle = Circle(42)
    val shape: Shape = circle
    val shapeParcelable: Parcelable = shape
    shapeParcelable.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val user2 = parcelableCreator<User>().createFromParcel(parcel)
    assert(user == user2)

    val circle2 = parcelableCreator<Circle>().createFromParcel(parcel)
    assert(circle == circle2)
}
