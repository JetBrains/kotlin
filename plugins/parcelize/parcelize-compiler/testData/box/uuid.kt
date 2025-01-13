// WITH_STDLIB

@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable
import kotlin.uuid.Uuid

@Parcelize
data class Test(
    val basic: Uuid,
    val nullable: Uuid?,
    val nil: Uuid,
) : Parcelable

fun box() = parcelTest { parcel ->
    val test = Test(
        basic = Uuid.fromLongs(10.toLong(), 12.toLong()),
        nullable = Uuid.fromLongs(11.toLong(), 13.toLong()),
        nil = Uuid.NIL
    )
    test.writeToParcel(parcel, 0)
    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)
    val got = parcelableCreator<Test>().createFromParcel(parcel)
    assert(test == got)
}
