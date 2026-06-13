package test

inline fun <T : Any> fooBound(x: T): Int = x.toString().length
inline fun <T> fooBody(x: T): Int = x.toString().length + 1
