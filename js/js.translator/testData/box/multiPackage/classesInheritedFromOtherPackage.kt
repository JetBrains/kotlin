// EXPECTED_REACHABLE_NODES: 504
// FILE: a.kt
package foo

import bar.*

open class A() {
    open fun f() = 3;
}

open class C() : B() {
    override fun f() = 5
}

fun box(): String {
    if (A().f() != 3) return "fail1"
    if (B().f() != 4) return "fail2"
    if (C().f() != 5) return "fail3"

    return "OK"
}


// FILE: b.kt
package bar

import foo.A

open class B() : A() {
    override fun f() = 4
}