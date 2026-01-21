// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class JHelp(var j1: String) {
    val j2 = 9
}

@Parcelize
class J(val j: @RawValue JHelp) : Parcelable

fun box() = parcelTest { parcel ->
    val test = J(JHelp("A"))

    try {
        test.writeToParcel(parcel, 0)
    } catch (e: IllegalArgumentException) {
        val isExpectedErrorMessage = e.message!!.contains("Parcel: unknown type for value test.JHelp")
        if (!isExpectedErrorMessage) {
            error("Unexpected error: ${e.toString()}")
        }
    }
}