// KIND: STANDALONE
// MODULE: SealedNaming
// FILE: nameing.kt

// A case name is the shortest suffix of the qualified name that is unique among the root's
// inheritors, so two inheritors sharing the simple name `Inner` must not collapse onto one case.

package org.kotlin.foo

sealed class MySealedClass

class MyClassA {
    sealed class Inner : MySealedClass() {
        class Leaf : Inner() {
            override fun toString(): String = "MyClassA.Inner.Leaf"
        }
    }
}

class MyClassB {
    sealed class Inner : MySealedClass() {
        class Leaf : Inner() {
            override fun toString(): String = "MyClassB.Inner.Leaf"
        }
    }
}

// KT-88806: `Foo_Bar` and `FooBar` both convert to the camelCase case name `fooBar`.
sealed interface SeparatorSealed

class Foo_Bar : SeparatorSealed {
    override fun toString(): String = "Foo_Bar"
}

class FooBar : SeparatorSealed {
    override fun toString(): String = "FooBar"
}

// FILE: factories.kt
package org.kotlin.foo

fun createMyClassALeaf(): MySealedClass = MyClassA.Inner.Leaf()

fun createMyClassBLeaf(): MySealedClass = MyClassB.Inner.Leaf()

fun createFooUnderscoreBar(): SeparatorSealed = Foo_Bar()

fun createFooBar(): SeparatorSealed = FooBar()
