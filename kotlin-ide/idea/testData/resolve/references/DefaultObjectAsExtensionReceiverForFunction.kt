package t

class A {
    companion object Companion {

    }
}

fun A.Companion.foo() {}

fun test() {
    <caret>A.foo()
}


// REF: companion object of (t).A

