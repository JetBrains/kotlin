// EXPECTED_REACHABLE_NODES: 497
// See https://youtrack.jetbrains.com/issue/KT-10785
package foo

class A(var x: Int) {
    operator fun plusAssign(other: A) {
        x += other.x
    }
}

object B {
    private var holder = A(42)

    val foo: A
        get() = holder
}

fun box(): String {
    B.foo += A(23)
    if (B.foo.x != 65) return "failed: ${B.foo.x}"
    return "OK"
}