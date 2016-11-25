package foo

external fun returnFalse(): Boolean = noImpl

fun box() = if (!returnFalse()) "OK" else "fail"
