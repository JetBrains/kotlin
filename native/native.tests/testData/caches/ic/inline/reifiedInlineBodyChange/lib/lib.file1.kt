package test

inline fun <reified T> foo(x: Any): Boolean = x is T
