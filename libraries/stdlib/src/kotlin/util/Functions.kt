package kotlin

/**
 * Converts a function that takes one argument and returns a value of the same type to a generator function.
 * The generator function calls this function, passing to it either [initialValue] on the first iteration
 * or the previously returned value on subsequent iterations, and returns the returned value.
 */
public fun <T: Any> Function1<T, T?>.toGenerator(initialValue: T): Function0<T?> {
    var nextValue: T? = initialValue
    return {
        nextValue?.let { result ->
            nextValue = this@toGenerator(result)
            result
        }
    }
}

