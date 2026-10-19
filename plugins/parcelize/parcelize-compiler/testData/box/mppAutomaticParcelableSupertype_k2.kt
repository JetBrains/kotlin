// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// DIAGNOSTICS: -EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE

// MODULE: m1-common
// FILE: common.kt

package test

import kotlinx.parcelize.Parcelize

expect interface CommonParcelable
annotation class TriggerParcelize

@Parcelize
data class BareUser(val name: String)

@TriggerParcelize
data class CustomUser(val name: String)

// The test harness checks common declarations again in metadata mode, where the
// actual typealias for CommonParcelable is not available.
@Parcelize
data class <!NO_PARCELABLE_SUPERTYPE{METADATA}!>WrappedUser<!>(val name: String) : CommonParcelable

@Parcelize
enum class AutomaticEnum {
    Entry,
}

// MODULE: m2-jvm()()(m1-common)
// FILE: android.kt

package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

actual typealias CommonParcelable = android.os.Parcelable

@Parcelize
data class AndroidUser(val name: String)

@TriggerParcelize
data class AndroidCustomUser(val name: String)

@Parcelize
data class ExplicitAndroidUser(val name: String) : Parcelable

// MODULE: m3-jvm(m2-jvm)
// FILE: main.kt

@file:JvmName("TestKt")

package test

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.parcelableCreator

private inline fun <reified T : Parcelable> roundTrip(value: T): T {
    val parcel = Parcel.obtain()
    try {
        value.writeToParcel(parcel, 0)
        val bytes = parcel.marshall()
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        return parcelableCreator<T>().createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
}

fun box(): String {
    val bare = BareUser("bare")
    val custom = CustomUser("custom")
    val wrapped = WrappedUser("wrapped")
    val android = AndroidUser("android")
    val androidCustom = AndroidCustomUser("android-custom")
    val explicitAndroid = ExplicitAndroidUser("explicit-android")

    assert(roundTrip(bare) == bare)
    assert(roundTrip(custom) == custom)
    assert(roundTrip(wrapped) == wrapped)
    assert(roundTrip(android) == android)
    assert(roundTrip(androidCustom) == androidCustom)
    assert(roundTrip(explicitAndroid) == explicitAndroid)
    assert(roundTrip(AutomaticEnum.Entry) == AutomaticEnum.Entry)

    return "OK"
}
