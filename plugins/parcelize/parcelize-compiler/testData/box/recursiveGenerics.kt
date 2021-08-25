// IGNORE_BACKEND: JVM
// See https://issuetracker.google.com/178321044
// WITH_RUNTIME

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Test(val sealedClass1 : SealedClass1<*, *>) : Parcelable

sealed class SealedClass1<out SC1 : SealedClass1<SC1, SC2>, out SC2 : SealedClass2<SC1, SC2>>(val sealedClass2Type: Class<out SC2>) : Parcelable {
    @Parcelize
    object SealedClass1Impl1 : SealedClass1<SealedClass1Impl1, SealedClass2.SealedClass2Impl1>(SealedClass2.SealedClass2Impl1::class.java)
}

sealed class SealedClass2<out SC1 : SealedClass1<SC1, SC2>, out SC2 : SealedClass2<SC1, SC2>> : Parcelable {
    @Parcelize
    object SealedClass2Impl1 : SealedClass2<SealedClass1.SealedClass1Impl1, SealedClass2.SealedClass2Impl1>()
}

fun box() = parcelTest { parcel ->
    val test = Test(SealedClass1.SealedClass1Impl1)
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = readFromParcel<Test>(parcel)
    assert(test == test2)
}
