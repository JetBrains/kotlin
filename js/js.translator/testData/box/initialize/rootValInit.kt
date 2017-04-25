// EXPECTED_REACHABLE_NODES: 495
package foo

class A(val a: Int)

class Empty

val x = 1
val a = A(2)
val e = Empty()

fun box(): String {
    if (x != 1) return "x != 1, it: $x"
    if (a.a != 2) return "a.a != 2, it: ${a.a}"

    return "OK"
}