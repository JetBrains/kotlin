package utils

inline
public fun <T, R> apply(x: T, fn: (T)->R): R =
        fn(x)