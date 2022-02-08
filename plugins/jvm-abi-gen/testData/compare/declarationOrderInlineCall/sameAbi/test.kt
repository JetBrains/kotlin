package test

fun g() = f()

inline fun f() = 1

// This does not change the line numbers in f, but does change the line
// numbers in the SourceDebug extension section which has to be rewritten.