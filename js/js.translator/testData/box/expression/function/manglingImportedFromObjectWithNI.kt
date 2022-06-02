// EXPECTED_REACHABLE_NODES: 1329
// Issue: KT-35904

import A.bar
import A.bar2

object A {
    inline fun <T> bar(x: T) = 42
    inline val <T> T.bar2 get() = 42
}

fun <T> T.foo1(): Int = bar(this)
fun <T> T.foo2(): Int = this.bar2

fun box(): String {
    10.foo1()
    10.foo2()

    return "OK"
}