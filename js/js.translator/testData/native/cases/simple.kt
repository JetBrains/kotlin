package foo

native
fun returnFalse(): Boolean = noImpl

fun box() = !returnFalse()
