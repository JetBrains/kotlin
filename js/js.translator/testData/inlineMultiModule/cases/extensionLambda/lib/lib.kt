package utils

inline
public fun apply<T, R>(x: T, fn: T.()->R): R =
        x.fn()
