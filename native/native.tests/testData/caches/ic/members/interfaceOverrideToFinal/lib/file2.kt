package test

fun viaInterface(i: I): Int = i.f()
fun viaClass(c: C): Int = c.f()
