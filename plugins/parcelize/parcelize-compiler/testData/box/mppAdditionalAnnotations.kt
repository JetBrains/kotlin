// TARGET_BACKEND: JVM_IR
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// Metadata compilations do not see through the expect/actuals and therefore,
// there are parcelize errors in metadata compilations.
// IGNORE_FIR_DIAGNOSTICS_DIFF

// MODULE: m1-common
// FILE: common.kt

package test

expect interface MyParcelable
annotation class TriggerParcelize

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
expect annotation class MyIgnore()
expect interface MyParceler<T>

@Retention(AnnotationRetention.SOURCE)
@Repeatable
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
expect annotation class MyTypeParceler<T, P : MyParceler<in T>>()
expect object SpecialStringParceler : MyParceler<String>

@TriggerParcelize
data class User(
    val i: Int,
    @MyTypeParceler<String, SpecialStringParceler> val s: String,
    @MyIgnore val name: String = "test",
) : MyParcelable


// MODULE: m2-jvm()()(m1-common)
// FILE: android.kt

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*

actual typealias MyParcelable = android.os.Parcelable
actual typealias MyIgnore = IgnoredOnParcel
actual typealias MyParceler<T> = Parceler<T>
actual typealias MyTypeParceler<T, P> = TypeParceler<T, P>

actual object SpecialStringParceler : Parceler<String> {
    override fun String.write(parcel: android.os.Parcel, flags: Int) {
        parcel.writeString(this)
    }

    override fun create(parcel: android.os.Parcel): String {
        return "HELLO " + parcel.readString()
    }
}

fun box() = parcelTest { parcel ->
    val user = User(1, "John", "John")
    user.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val user2 = parcelableCreator<User>().createFromParcel(parcel)
    assert(user.i == user2.i)
    assert(user.name != user2.name)
    assert(user2.s == "HELLO John")
}