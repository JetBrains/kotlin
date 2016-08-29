package foo

@native
fun returnFalse(): Boolean = noImpl

fun box() = if (!returnFalse()) "OK" else "fail"
