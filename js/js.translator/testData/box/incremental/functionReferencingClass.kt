// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1285
// FILE: A.kt

open class A {
    open fun f(): Int = 1
}

// FILE: useA.kt
// RECOMPILE

fun useA(a: A): Int = a.f()
fun useList(xs: List<Int>) {}

// FILE: box.kt

fun box(): String {
    val result = useA(A())
    if (result != 1) return "fail: result of 'useA(A())' is '$result', but '1' was expected"

    return "OK"
}