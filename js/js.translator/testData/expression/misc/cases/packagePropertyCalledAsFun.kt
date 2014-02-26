package foo

fun lold() = true

val p = { { lold() }() }

fun box() = p() && foo.p()