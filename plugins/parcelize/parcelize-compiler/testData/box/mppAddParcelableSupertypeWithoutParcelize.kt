// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

package test

import kotlinx.parcelize.*

@AddParcelableSupertype
sealed interface Shape

@AddParcelableSupertype
abstract class Named {
    abstract val name: String
}

@Parcelize
data class Circle(val radius: Int) : Shape

@Parcelize
data class Square(val side: Int) : Shape

// MODULE: m2-jvm()()(m1-common)
// FILE: android.kt

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
data class Person(override val name: String) : Named()

@Parcelize
data class Holder(val shape: Shape, val named: Named) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Holder(Circle(3), Person("John"))
    val shapeAsParcelable: Parcelable = first.shape
    val namedAsParcelable: Parcelable = first.named
    assert(shapeAsParcelable is Circle && namedAsParcelable is Person)

    first.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val second = parcelableCreator<Holder>().createFromParcel(parcel)
    assert(first == second)
}
