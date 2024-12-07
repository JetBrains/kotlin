// IGNORE_BACKEND: JVM
// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable
import android.util.*

object TrueParceler: Parceler<Boolean> {
    override fun create(parcel: Parcel) = true
    override fun Boolean.write(parcel: Parcel, flags: Int) {}
}

@Parcelize
data class Data(val a: String, val b: String) : Parcelable

@Parcelize
class Box(
    val a: SparseBooleanArray?,
    val b: SparseIntArray?,
    val c: SparseLongArray?,
    val d: SparseArray<Data>?,
    val e: SparseArray<Data?>?
) : Parcelable

@Parcelize
@TypeParceler<Boolean, TrueParceler>
class TrueBox(val value: SparseBooleanArray?) : Parcelable

fun box() = parcelTest { parcel ->
    val boxNonNull = Box(
        SparseBooleanArray().apply { put(1, true); put(100, false) },
        SparseIntArray().apply { put(1, 5); put(100, -1); put(1000, 0) },
        SparseLongArray().apply { put(3, 2); put(2, 3); put(10, 10) },
        SparseArray<Data>().apply { put(1, Data("A", "B")); put(10, Data("C", "D")); put(105, Data("E", "")) },
        SparseArray<Data?>().apply { put(1, Data("A", "B")); put(10, null) }
    )
    val boxNull = Box(null, null, null, null, null)
    val trueBoxNonNull = TrueBox(SparseBooleanArray().apply { put(1, false) })
    val trueBoxNull = TrueBox(null)

    boxNonNull.writeToParcel(parcel, 0)
    boxNull.writeToParcel(parcel, 0)
    trueBoxNonNull.writeToParcel(parcel, 0)
    trueBoxNull.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val boxNonNull2 = parcelableCreator<Box>().createFromParcel(parcel)
    assert(compareBoxes(boxNonNull, boxNonNull2))

    val boxNull2 = parcelableCreator<Box>().createFromParcel(parcel)
    assert(compareBoxes(boxNull, boxNull2))

    val trueSparseArray = parcelableCreator<TrueBox>().createFromParcel(parcel).value
    assert(trueSparseArray != null)
    assert(trueSparseArray?.size() == 1)
    assert(trueSparseArray?.keyAt(0) == 1)
    assert(trueSparseArray?.valueAt(0) == true)

    val trueBoxNull2 = parcelableCreator<TrueBox>().createFromParcel(parcel)
    assert(trueBoxNull2.value == null)
}

private fun compareBoxes(first: Box, second: Box): Boolean {
    if (!compareSparseBooleanArrays(first.a, second.a)) return false
    if (!compareSparseIntArrays(first.b, second.b)) return false
    if (!compareSparseLongArrays(first.c, second.c)) return false
    if (!compareSparseArrays(first.d, second.d)) return false
    return compareSparseArrays(first.e, second.e)
}

private fun compareSparseBooleanArrays(first: SparseBooleanArray?, second: SparseBooleanArray?): Boolean {
    if (first === second) return true
    if (first == null || second == null) return false
    if (first.size() != second.size()) return false

    for (i in 0 until first.size()) {
        if (first.keyAt(i) != second.keyAt(i)) return false
        if (first.valueAt(i) != second.valueAt(i)) return false
    }

    return true
}

private fun compareSparseIntArrays(first: SparseIntArray?, second: SparseIntArray?): Boolean {
    if (first === second) return true
    if (first == null || second == null) return false
    if (first.size() != second.size()) return false

    for (i in 0 until first.size()) {
        if (first.keyAt(i) != second.keyAt(i)) return false
        if (first.valueAt(i) != second.valueAt(i)) return false
    }

    return true
}

private fun compareSparseLongArrays(first: SparseLongArray?, second: SparseLongArray?): Boolean {
    if (first === second) return true
    if (first == null || second == null) return false
    if (first.size() != second.size()) return false

    for (i in 0 until first.size()) {
        if (first.keyAt(i) != second.keyAt(i)) return false
        if (first.valueAt(i) != second.valueAt(i)) return false
    }

    return true
}

private fun compareSparseArrays(first: SparseArray<*>?, second: SparseArray<*>?): Boolean {
    if (first === second) return true
    if (first == null || second == null) return false
    if (first.size() != second.size()) return false

    for (i in 0 until first.size()) {
        if (first.keyAt(i) != second.keyAt(i)) return false
        if (first.valueAt(i) != second.valueAt(i)) return false
    }

    return true
}
