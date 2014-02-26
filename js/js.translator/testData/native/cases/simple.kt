package foo

import js.*

native
fun returnFalse(): Boolean = js.noImpl

fun box() = !returnFalse()