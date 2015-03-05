package kotlin

/**
 * Returns an array with the specified [size], where each element is calculated by calling the specified
 * [init] function. The `init` function returns an array element given its index.
 */
public inline fun <reified T> Array(size: Int, init: (Int) -> T): Array<T> {
    val result = arrayOfNulls<T>(size)

    for (i in 0..size - 1) {
        result[i] = init(i)
    }

    return result as Array<T>
}
