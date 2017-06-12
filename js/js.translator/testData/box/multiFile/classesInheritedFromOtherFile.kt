// EXPECTED_REACHABLE_NODES: 504
// FILE: a.kt
package foo

open class A() {
    open fun f() = 3;
}

open class C() : B() {
    override fun f() = 5
}


// FILE: b.kt
package foo

open class B() : A() {
    override fun f() = 4
}

fun box(): String {
    if (A().f() != 3) return "fail1"
    if (B().f() != 4) return "fail2"
    if (C().f() != 5) return "fail3"

    return "OK"
}