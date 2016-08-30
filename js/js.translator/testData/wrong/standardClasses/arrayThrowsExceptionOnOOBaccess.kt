package foo

class A() {
}

val a1 = Array<Int>(3)
val a2 = Array<A>(2)

fun box() = if (a1[4] == null) "OK" else "fail"