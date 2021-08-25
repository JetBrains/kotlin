// See KT-44891, https://issuetracker.google.com/180193969
// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class Covariant<out T : CharSequence>(val block: () -> T) : Parcelable

@Parcelize
class Contravariant<in T : CharSequence>(val block: (T) -> Boolean) : Parcelable

@Parcelize
class Invariant<T : CharSequence>(val s: CharSequence) : Parcelable

fun box() = parcelTest { parcel ->
    val covariant1 = Covariant<String> { "OK" }
    val contravariant1 = Contravariant<String> { it == "OK" }
    val invariant1 = Invariant<String>("OK")
    covariant1.writeToParcel(parcel, 0)
    contravariant1.writeToParcel(parcel, 0)
    invariant1.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val covariant2 = readFromParcel<Covariant<String>>(parcel)
    assert(covariant2.block() == "OK")

    val contravariant2 = readFromParcel<Contravariant<String>>(parcel)
    assert(contravariant2.block("OK"))

    val invariant2 = readFromParcel<Invariant<String>>(parcel)
    assert(invariant2.s.toString() == "OK")
}
