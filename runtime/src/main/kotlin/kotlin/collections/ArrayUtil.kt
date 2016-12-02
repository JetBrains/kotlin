package kotlin.collections

/**
 * Returns an array of objects of the given type with the given [size], initialized with **lateinit** _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
fun <E> arrayOfLateInitElements(size: Int): Array<E> {
   // TODO: maybe use an empty array.
   // return (if (size == 0) emptyArray else Array<Any>(size)) as Array<E>
   return Array<E>(size)
}

/**
 * Returns new array which is a copy of the original array with new elements filled with **lateinit** _uninitialized_ values.
 * Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
fun <E> Array<E>.copyOfLateInitElements(newSize: Int): Array<E> {
    val result = Array<E>(newSize)
    this.copyRangeTo(result, 0, if (newSize > this.size) this.size else newSize, 0)
    return result
}

/**
 * Resets an array element at a specified index to some implementation-specific _uninitialized_ value.
 * In particular, references stored in this element are released and become available for garbage collection.
 * Attempts to read _uninitialized_ value work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
fun <E> Array<E>.resetAt(index: Int) {
    (this as Array<Any?>)[index] = null
}

@SymbolName("Kotlin_Array_fillImpl")
external private fun fillImpl(array: Array<Any>, fromIndex: Int, toIndex: Int, value: Any?)

/**
 * Resets a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to some implementation-specific _uninitialized_ value.
 * In particular, references stored in these elements are released and become available for garbage collection.
 * Attempts to read _uninitialized_ values work in implementation-dependent manner,
 * either throwing exception or returning some kind of implementation-specific default value.
 */
fun <E> Array<E>.resetRange(fromIndex: Int, toIndex: Int) {
    fillImpl(this as Array<Any>, fromIndex, toIndex, null)
}

@SymbolName("Kotlin_Array_copyImpl")
external private fun copyImpl(array: Array<Any>, fromIndex: Int,
                         destination: Array<Any>, toIndex: Int, count: Int)

/**
 * Copies a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to another [destination] array starting at [destinationIndex].
 */
fun <E> Array<E>.copyRangeTo(destination: Array<E>, fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyImpl(this as Array<Any>, fromIndex, destination as Array<Any>, destinationIndex, toIndex - fromIndex)
}

/**
 * Copies a range of array elements at a specified [fromIndex] (inclusive) to [toIndex] (exclusive) range of indices
 * to another part of this array starting at [destinationIndex].
 */
fun <E> Array<E>.copyRange(fromIndex: Int, toIndex: Int, destinationIndex: Int = 0) {
    copyRangeTo(this, fromIndex, toIndex, destinationIndex)
}