package kotlin

/**
 * Get the first element in the list or throws [[EmptyIterableException]] if list is empty.
 */
public inline fun <T> List<T>.first() : T {
    return if (size() > 0) get(0) else throw EmptyIterableException(this)
}

/**
 * Get the first element in the list or *null* if list is empty.
 */
public inline fun <T:Any> List<T>.firstOrNull() : T? {
    return if (size() > 0) get(0) else null
}

/**
 * Get the last element in the list or throws [[EmptyIterableException]] if list is empty.
 */
public inline fun <T> List<T>.last() : T {
    val s = size()
    return if (s > 0) get(s - 1) else throw EmptyIterableException(this)
}

/**
 * Get the last element in the list or *null* if list is empty.
 */
public inline fun <T:Any> List<T>.lastOrNull() : T? {
    val s = size()
    return if (s > 0) get(s - 1) else null
}

public inline fun <T> List<T>.forEachWithIndex(operation : (Int, T) -> Unit) {
    for (index in indices) {
        operation(index, get(index))
    }
}

/**
 * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <T, R> List<T>.foldRight(initial: R, operation: (T, R) -> R) : R {
    var r = initial
    var index = size - 1

    while (index >= 0) {
        r = operation(get(index--), r)
    }

    return r
}

/**
 * Applies binary operation to all elements of iterable, going from right to left.
 * Similar to foldRight function, but uses the last element as initial value
 */
public inline fun <T> List<T>.reduceRight(operation: (T, T) -> T) : T {
    var index = size - 1
    if (index < 0) {
        throw UnsupportedOperationException("Empty iterable can't be reduced")
    }

    var r = get(index--)
    while (index >= 0) {
        r = operation(get(index--), r)
    }

    return r
}

/**
 * Returns a original Iterable containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
 */
public inline fun <T:Any> List<T?>.requireNoNulls() : List<T> {
    for (element in this) {
        if (element == null) {
            throw IllegalArgumentException("null element found in $this")
        }
    }
    return this as List<T>
}

