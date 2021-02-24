// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import android.text.SpannableString

@Parcelize
data class Test(val simple: CharSequence, val spanned: CharSequence) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test("John", SpannableString("Smith"))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<Test>(parcel)

    assert(test.simple.toString() == test2.simple.toString())
    assert(test.spanned.toString() == test2.spanned.toString())
}