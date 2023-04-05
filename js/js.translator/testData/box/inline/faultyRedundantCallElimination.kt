// EXPECTED_REACHABLE_NODES: 1311

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

// CHECK_BREAKS_COUNT: function=box count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=box name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun box() = O() + K(a)
