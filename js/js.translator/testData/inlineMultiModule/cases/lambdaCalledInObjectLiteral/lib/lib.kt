package utils

inline
public fun <T, R> apply(x: T, crossinline fn: (T)->R): R {
    val result = object {
        val x = fn(x)
    }

    return result.x
}