// https://issuetracker.google.com/TODO
// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import android.os.Parcelable
import kotlinx.parcelize.*

data class NonNullBox<T>(val x: T & Any)

@Parcelize
data class Test(
    val nested: @DataClass NonNullBox<NonNullBox<Int>>,
    val nullableArgument: @DataClass NonNullBox<String?>,
) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test(NonNullBox(NonNullBox(1)), NonNullBox("x"))
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Test>().createFromParcel(parcel)
    check(test == test2) { "Expected $test, got $test2" }
    check(parcel.dataAvail() == 0) { "Parcel has ${parcel.dataAvail()} unread bytes" }
}
