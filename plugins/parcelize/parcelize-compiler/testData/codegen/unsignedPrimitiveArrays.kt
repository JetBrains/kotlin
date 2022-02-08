// CURIOUS_ABOUT: writeToParcel, createFromParcel
// WITH_STDLIB
// IGNORE_BACKEND: JVM

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
data class Test(
    val a: UByteArray,
    val b: UShortArray,
    val c: UIntArray,
    val d: ULongArray,
    val e: UByteArray?,
    val f: UShortArray?,
    val g: UIntArray?,
    val h: ULongArray?,
) : Parcelable
