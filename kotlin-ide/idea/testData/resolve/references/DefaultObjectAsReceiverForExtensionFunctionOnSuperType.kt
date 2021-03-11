package t

interface Trait

open class A {
    companion object Companion : Trait {

    }
}

fun Trait.foo() {}

fun test() {
    <caret>A.foo()
}


// REF: companion object of (t).A

