// EXPECTED_REACHABLE_NODES: 995
// MODULE: lib
// FILE: lib.kt

class A {
    fun ok() = "OK"
}

val A.foo: String
    get() = ok()

// MODULE: main(lib)
// FILE: main.kt

fun box() = A().foo