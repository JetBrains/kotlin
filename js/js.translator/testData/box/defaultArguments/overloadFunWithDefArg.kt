// EXPECTED_REACHABLE_NODES: 499
package foo

open abstract class B() {
    abstract fun foo2(arg: Int = 239): Int
}

class C() : B() {
    override fun foo2(arg: Int): Int = arg
}

fun box(): String {
    if (C().foo2() != 239) return "fail1"
    if (C().foo2(10) != 10) return "fail2"
    return "OK"
}
