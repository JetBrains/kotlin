package test

fun g() = f()

inline fun f() = 1

// The line numbers in f are the same, but the SourceDebug extension section
// contains a different line number for the inline function call in g.
// g is not inline, though, so the debugging information for its body is removed.