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
