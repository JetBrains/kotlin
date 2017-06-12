// EXPECTED_REACHABLE_NODES: 497
package foo

open abstract class B() {
    fun foo(arg: Int = 239 + 1): Int = arg
}

class C() : B() {
}

fun box(): String {
    if (C().foo(10) != 10) return "fail1"
    if (C().foo() != 240) return "fail2"
    return "OK"
}
