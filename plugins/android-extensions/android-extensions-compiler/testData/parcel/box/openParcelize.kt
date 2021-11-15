// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
open class Base(val a: String) : Parcelable

@Parcelize
class Inh(var b: Int) : Base(""), Parcelable

fun box(): String {
    Inh(0)
    return "OK"
}