// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import android.os.Bundle

@Parcelize
data class User(val a: Bundle) : Parcelable

fun box() = parcelTest { parcel ->
    val test = User(Bundle().apply { putChar("A", 'c'); putByte("B", 40.toByte()); putString("C", "ABC") })
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<User>(parcel)

    assert(compareBundles(test.a, test2.a))
}

private fun compareBundles(first: Bundle, second: Bundle): Boolean {
    if (first.size() != second.size()) return false

    for (key in first.keySet()) {
        if (first.get(key) != second.get(key)) return false
    }

    return true
}