package foo

native
fun returnFalse(): Boolean = js.noImpl

fun box() = !returnFalse()
