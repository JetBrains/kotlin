// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class JHelp(var j1: String) {
    val j2 = 9
}

@Suppress("DEPRECATED_ANNOTATION")
@Parcelize
class J(val j: @RawValue JHelp) : Parcelable

fun box() = parcelTest { parcel ->
    val test = J(JHelp("A"))

    var exceptionCaught = false
    try {
        test.writeToParcel(parcel, 0)
    } catch (e: RuntimeException) {
        if (e.message!!.contains("Parcel: unable to marshal value test.JHelp")) {
            exceptionCaught = true
        } else {
            throw e
        }
    }

    if (!exceptionCaught) {
        error("Exception should be thrown")
    }
}