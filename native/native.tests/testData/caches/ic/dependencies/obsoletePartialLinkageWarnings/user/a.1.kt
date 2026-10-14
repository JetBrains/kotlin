package user

import lib.callTarget
import lib.plain

fun inlinesFromLib(): Int = callTarget() + 1

// Keeps the dependency of this file on `lib` even when the inlined code above is replaced
// with a linkage error.
fun callsLib(): Int = plain()
