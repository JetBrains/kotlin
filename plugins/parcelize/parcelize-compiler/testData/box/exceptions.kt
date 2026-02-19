// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
class ExceptionContainer(val exn: Exception) : Parcelable

fun box() = parcelTest { parcel ->
    val test = ExceptionContainer(java.lang.RuntimeException("Don't throw me."))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    parcelableCreator<ExceptionContainer>().createFromParcel(parcel)
}
