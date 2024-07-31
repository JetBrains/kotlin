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

fun box(): String {
    if (A("O").s + A('K').s != "OK") return "fail"

    return ok()
}
