// EXPECTED_REACHABLE_NODES: 1276
// FILE: lib.kt

// Force constructor renaming
val dummy = run {
    if (false) {
        js("A")
        js("A_init")
    }
    null
}

class A(val s: String) {
    constructor(c: Char) : this("$c")
}

inline fun ok() = A("O").s + A('K').s

// FILE: main.kt


// TODO add directives for primary constructor invocations
// CHECK_CALLED_IN_SCOPE: function=A_init_0 scope=box
// CHECK_NOT_CALLED_IN_SCOPE: function=A_init scope=box
fun box(): String {
    if (A("O").s + A('K').s != "OK") return "fail"

    return ok()
}
