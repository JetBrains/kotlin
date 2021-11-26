// WITH_STDLIB

@file:JvmName("TestKt")
package test

import kotlinx.android.parcel.*
import android.os.Parcel
import android.os.Parcelable
import android.util.*

@Parcelize
data class Data(val a: String, val b: String) : Parcelable

@Parcelize
data class User(val a: SparseIntArray, val b: SparseLongArray, val c: SparseArray<Data>) : Parcelable

fun box() = parcelTest { parcel ->
    val user = User(
            a = SparseIntArray().apply { put(1, 5); put(100, -1); put(1000, 0) },
            b = SparseLongArray().apply { put(3, 2); put(2, 3); put(10, 10) },
            c = SparseArray<Data>().apply { put(1, Data("A", "B")); put(10, Data("C", "D")); put(105, Data("E", "")) }
    )

    user.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val user2 = readFromParcel<User>(parcel)

    assert(compareSparseIntArrays(user.a, user2.a))
    assert(compareSparseLongArrays(user.b, user2.b))
    assert(compareSparseArrays(user.c, user2.c))
}

private fun compareSparseIntArrays(first: SparseIntArray, second: SparseIntArray): Boolean {
    if (first === second) return true
    if (first.size() != second.size()) return false

    for (i in 0 until first.size()) {
        if (first.keyAt(i) != second.keyAt(i)) return false
        if (first.valueAt(i) != second.valueAt(i)) return false
    }

    return true
}

private fun compareSparseLongArrays(first: SparseLongArray, second: SparseLongArray): Boolean {
    if (first === second) return true
    if (first.size() != second.size()) return false

    for (i in 0 until first.size()) {
        if (first.keyAt(i) != second.keyAt(i)) return false
        if (first.valueAt(i) != second.valueAt(i)) return false
    }

    return true
}

private fun compareSparseArrays(first: SparseArray<*>, second: SparseArray<*>): Boolean {
    if (first === second) return true
    if (first.size() != second.size()) return false

    for (i in 0 until first.size()) {
        if (first.keyAt(i) != second.keyAt(i)) return false
        if (first.valueAt(i) != second.valueAt(i)) return false
    }

    return true
}