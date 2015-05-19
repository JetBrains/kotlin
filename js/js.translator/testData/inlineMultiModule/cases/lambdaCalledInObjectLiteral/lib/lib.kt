package utils

inline
public fun apply<T, R>(x: T, inlineOptions(InlineOption.ONLY_LOCAL_RETURN) fn: (T)->R): R {
    val result = object {
        val x = fn(x)
    }

    return result.x
}