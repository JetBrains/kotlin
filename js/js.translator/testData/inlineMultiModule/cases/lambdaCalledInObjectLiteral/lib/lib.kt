package utils

inline
public fun apply<T, R>(x: T, crossinline fn: (T)->R): R {
    val result = object {
        val x = fn(x)
    }

    return result.x
}