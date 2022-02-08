// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize class TestParcel : Parcelable {
    companion object : Parceler<TestParcel> {
        override fun create(parcel: Parcel): TestParcel {
            return TestParcel()
        }
        override fun TestParcel.write(parcel: Parcel, flags: Int) {}
    }
}

fun box() = parcelTest {
    TestParcel()
}