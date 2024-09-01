// CURIOUS_ABOUT: writeToParcel, createFromParcel, <init>
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable


@Parcelize
open class Base(val x: String): Parcelable

@Parcelize
open class Derive1(val y: Int, x: String): Base(x)

@Parcelize
class Derive2(val z: Double, y: Int, x: String): Derive1(y, x)