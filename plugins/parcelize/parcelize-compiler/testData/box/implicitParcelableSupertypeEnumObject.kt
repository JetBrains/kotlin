// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
enum class Color(val rgba8: Long) { BLACK(0x000000FFL), WHITE(0xFFFFFFFFL) }

@Parcelize
object Obj

fun box() = parcelTest { parcel ->
    val black: Parcelable = Color.BLACK
    val obj: Parcelable = Obj

    black.writeToParcel(parcel, 0)
    obj.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val black2 = parcelableCreator<Color>().createFromParcel(parcel)
    val obj2 = parcelableCreator<Obj>().createFromParcel(parcel)

    assert(black2 == Color.BLACK)
    assert(black2.rgba8 == 0x000000FFL)
    assert(obj2 === Obj)
}
