// MODULE: main
// FILE: lib.kt
@file:OptIn(ExperimentalStdlibApi::class)

external open class Foo {
    @JsSymbol("toStringTag")
    open fun foo(): String
}

class Bar : Foo() {
    override fun foo() = "O${super.foo()}"
}

fun box(): String {
    val foo = Foo()
    val bar = Bar()

    var test = foo.foo()

    if (test != "K") return "Failed 1: $test"

    test = foo.asDynamic()[js("Symbol.toStringTag")]()

    if (test != "K") return "Failed 2: $test"

    test = bar.foo()

    if (test != "OK") return "Failed 3: $test"

    test = bar.asDynamic()[js("Symbol.toStringTag")]()

    if (test != "OK") return "Failed 4: $test"

    return "OK"
}