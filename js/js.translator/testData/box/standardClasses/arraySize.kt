// EXPECTED_REACHABLE_NODES: 494
package foo

class A() {
}

val a1 = arrayOfNulls<Int>(3)
val a2 = arrayOfNulls<A>(2)

fun box() = if (a1.size == 3 && a2.size == 2) "OK" else "fail"