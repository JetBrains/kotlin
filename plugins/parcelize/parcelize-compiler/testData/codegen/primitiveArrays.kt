// CURIOUS_ABOUT: writeToParcel, createFromParcel
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
/*
 * Serializing of short arrays is not properly supported in old frontend, so this
 *   test is on only for JVM IR backend
 */

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
