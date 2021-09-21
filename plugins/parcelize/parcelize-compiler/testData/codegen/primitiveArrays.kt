// CURIOUS_ABOUT writeToParcel, createFromParcel
// WITH_RUNTIME

// The JVM backend doesn't support ShortArray
// IGNORE_BACKEND: JVM

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
data class Test(
    val a: ByteArray,
    val b: CharArray,
    val c: ShortArray,
    val d: IntArray,
    val e: LongArray,
    val f: ByteArray?,
    val g: CharArray?,
    val h: ShortArray?,
    val i: IntArray?,
    val j: LongArray?,
    val k: FloatArray,
    val l: DoubleArray,
    val m: FloatArray?,
    val n: DoubleArray?
) : Parcelable
