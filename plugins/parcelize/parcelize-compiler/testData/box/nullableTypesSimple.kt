// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Test(val a: String?) : Parcelable

fun box() = parcelTest { parcel ->
    val first = Test("John")
    val second = Test(null)

    first.writeToParcel(parcel, 0)
    second.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val parcelableCreator = parcelableCreator<Test>()
    val first2 = parcelableCreator.createFromParcel(parcel)
    val second2 = parcelableCreator.createFromParcel(parcel)

    assert(first == first2)
    assert(second == second2)
}