package utils

inline
public fun <T, R> apply(x: T, fn: (T)->R): R {
    val y = fn(x)
    return y
}