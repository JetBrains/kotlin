// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable

data class Box<T>(val x: T)

@Parcelize
data class ParcelableNode(val x: Int, val next: ParcelableNode?) : Parcelable

data class ContainsParcelableNode(val node: ParcelableNode)

@Parcelize
data class Test(
    val nestedBox: @DataClass Box<@DataClass Box<Int>>,
    val containsParcelableNode: @DataClass ContainsParcelableNode,
) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test(
        nestedBox = Box(Box(42)),
        containsParcelableNode = ContainsParcelableNode(ParcelableNode(1, ParcelableNode(2, ParcelableNode(3, null)))),
    )
    test.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val test2 = parcelableCreator<Test>().createFromParcel(parcel)
    check(test == test2) { "Expected $test, got $test2" }
    check(parcel.dataAvail() == 0) { "Parcel has ${parcel.dataAvail()} unread bytes" }
}
