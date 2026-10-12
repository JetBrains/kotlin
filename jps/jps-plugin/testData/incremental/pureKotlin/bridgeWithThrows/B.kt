package test

inline fun retry(): Unit = consume(A { Unit })

fun <T> consume(operation: A<T>): T = operation()
