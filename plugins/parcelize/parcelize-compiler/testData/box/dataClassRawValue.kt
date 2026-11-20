// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable
import java.io.Serializable

// Data classes in a `@RawValue` type are still serialized property by property, in contrast to `writeSerializable`,
// which would drop the transient property of `Inner`.
data class Inner(val x: String, @Transient val transient: String?) : Serializable
data class Outer(val inner: @RawValue Inner)

// Unsupported types in such data classes are serialized with `writeValue`.
data class Box<T>(val x: T)

@Parcelize
class Test(
    val outer: @DataClass Outer,
    val inner: @RawValue @DataClass Inner,
    val box: @RawValue @DataClass Box<Any?>,
) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test(Outer(Inner("x", "transient")), Inner("y", "transient"), Box(listOf(1, 2)))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Test>().createFromParcel(parcel)
    check(test2.outer == test.outer) { "Expected ${test.outer}, got ${test2.outer}" }
    check(test2.inner == test.inner) { "Expected ${test.inner}, got ${test2.inner}" }
    check(test2.box == test.box) { "Expected ${test.box}, got ${test2.box}" }
    check(parcel.dataAvail() == 0) { "Parcel has ${parcel.dataAvail()} unread bytes" }
}
