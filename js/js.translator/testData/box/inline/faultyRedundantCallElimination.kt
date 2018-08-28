// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1189

// MODULE: lib
// FILE: lib.kt

fun String.call() = this + "O"

inline fun O() = "".call()

object A {
    val foo = Foo
}

object Foo {
    @JsName("call")
    fun call(a: A, k: String) = k
}

inline fun K(a: A) = a.foo.call(a, "K")

// MODULE: main(lib)
// FILE: main.kt

val a = A

fun box() = O() + K(a)