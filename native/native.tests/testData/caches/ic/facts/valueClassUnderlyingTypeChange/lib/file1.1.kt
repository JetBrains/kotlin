package test

value class V(val x: Long)

fun makeV(): V = V(5_000_000_007L)
fun readV(v: V): Long = v.x
