// EXPECTED_REACHABLE_NODES: 498
package foo

open class A() {
    open fun c() = 2
}

class B() : A() {
}

fun B.d() = c() + 3

fun box(): String {
    return if (B().d() == 5) "OK" else "fail"
}
