package test

value class V(val x: Int)

fun makeV(): V = V(7)
fun readV(v: V): Long = v.x.toLong()
