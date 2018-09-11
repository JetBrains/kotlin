// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1302
// MODULE: lib
// FILE: lib.kt

abstract class A {
    fun f() = o() + k()

    abstract fun o(): String

    abstract fun k(): String
}

inline fun foo(x: String): A {
    return object : A() {
        override fun o(): String = x

        override fun k(): String = "K"
    }
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String = foo("O").f()