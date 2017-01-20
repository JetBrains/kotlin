package kotlin.collections

/**
 * Returns an array of objects of the given type with the given [size], initialized with _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> arrayOfUninitializedElements(size: Int): Array<E> {
    // TODO: special case for size == 0?
    return Array<E>(size)
}

/**
 * Returns new array which is a copy of the original array with new elements filled with **lateinit** _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
fun <E> Array<E>.copyOfUninitializedElements(newSize: Int): Array<E> {
    val result = arrayOfUninitializedElements<E>(newSize)
    this.copyRangeTo(result, 0, if (newSize > this.size) this.size else newSize, 0)
    return result
}

fun IntArray.copyOfUninitializedElements(newSize: Int): IntArray {
    val result = IntArray(newSize)
    this.copyRangeTo(result, 0, if (newSize > this.size) this.size else newSize, 0)
    return result
}

/**
 * Resets an array element at a specified index to some implementation-specific _uninitialized_ value.
 * In particular, references stored in this element are released and become available for garbage collection.
 * Attempts to read _uninitialized_ value work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> Array<E>.resetAt(index: Int) {
    (@Suppress("UNCHECKED_CAST")(this as Array<Any?>))[index] = null
}

@SymbolName("Kotlin_Array_fillImpl")
external private fun fillImpl(array: Array<Any>, fromIndex: Int, toIndex: Int, value: Any?)

@SymbolName("Kotlin_IntArray_fillImpl")
external private fun fillImpl(array: IntArray, fromIndex: Int, toIndex: Int, value: Int)

/**
 * Resets a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to some implementation-specific _uninitialized_ value.
 * In particular, references stored in these elements are released and become available for garbage collection.
 * Attempts to read _uninitialized_ values work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
internal fun <E> Array<E>.resetRange(fromIndex: Int, toIndex: Int) {
    fillImpl(@Suppress("UNCHECKED_CAST") (this as Array<Any>), fromIndex, toIndex, null)
}

internal fun IntArray.fill(fromIndex: Int, toIndex: Int, value: Int) {
    fillImpl(this, fromIndex, toIndex, value)
}

@SymbolName("Kotlin_Array_copyImpl")
external private fun copyImpl(array: Array<Any>, fromIndex: Int,
                         destination: Array<Any>, toIndex: Int, count: Int)

@SymbolName("Kotlin_IntArray_copyImpl")
external private fun copyImpl(array: IntArray, fromIndex: Int,
                              destination: IntArray, toIndex: Int, count: Int)

/**
 * Copies a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to another [destination] array starting at [destinationIndex].
 */
fun <E> Array<E>.copyRangeTo(destination: Array<E>, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(@Suppress("UNCHECKED_CAST") (this as Array<Any>), fromIndex,
             @Suppress("UNCHECKED_CAST") (destination as Array<Any>),
             destinationIndex, toIndex - fromIndex)
}

fun IntArray.copyRangeTo(destination: IntArray, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this, fromIndex, destination, destinationIndex, toIndex - fromIndex)
}

/**
 * Copies a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to another part of this array starting at [destinationIndex].
 */
fun <E> Array<E>.copyRange(fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyRangeTo(this, fromIndex, toIndex, destinationIndex)
}

internal fun <E> Collection<E>.collectionToString(): String {
    val sb = StringBuilder(2 + size * 3)
    sb.append("[")
    var i = 0
    val it = iterator()
    while (it.hasNext()) {
        if (i > 0) sb.append(", ")
        val next = it.next()
        if (next == this) sb.append("(this Collection)") else sb.append(next)
        i++
    }
    sb.append("]")
    return sb.toString()
}