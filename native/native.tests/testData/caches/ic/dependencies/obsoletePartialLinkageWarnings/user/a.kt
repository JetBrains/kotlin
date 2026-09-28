package user

import lib.callTarget
import lib.plain

fun inlinesFromLib(): Int = callTarget()

// Keeps the dependency of this file on `lib` even when the inlined code above is replaced
// with a linkage error, so that the cache of this file is rebuilt once `lib` is repaired.
fun callsLib(): Int = plain()
