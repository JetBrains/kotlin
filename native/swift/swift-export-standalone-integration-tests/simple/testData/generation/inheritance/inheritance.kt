// KIND: STANDALONE
// MODULE: main(module)
// FILE: main.kt

open class Foo

private class Bar : Foo()

fun getFoo(): Foo = Bar()
var foo: Foo = Bar()

// MODULE: module
// FILE: foo.kt

package foo

open class Foo

private class Bar : Foo()

fun getFoo(): Foo = Bar()
var foo: Foo = Bar()

// KT-79227 Swift Export: Fix First Release Issues
// Override resolution doesn’t consider existential types
interface P {
    fun f()
}

open class Base {
    open fun g(x: P) {}
}

open class Sub : Base() {
    override fun g(x: P) {}
}
