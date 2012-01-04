package foo

class A() {}

val a1 = Array<Int>(3)
val a2 = Array<A>(2)

fun box() = (a1.size == 3 && a2.size == 2)