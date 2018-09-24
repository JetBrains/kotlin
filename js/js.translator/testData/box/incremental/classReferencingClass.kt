// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1292
// FILE: A.kt

open class A {
    open fun f(): Int = 1
}

// FILE: B.kt
// RECOMPILE

class B : A() {
    override fun f(): Int = super.f() + 2
}

// FILE: box.kt

fun box(): String {
    val af = A().f()
    if (af != 1) return "fail: result of 'A().f()' is '$af', but '1' was expected"

    val bf = B().f()
    if (bf != 3) return "fail: result of 'B().f()' is '$bf', but '3' was expected"

    return "OK"
}