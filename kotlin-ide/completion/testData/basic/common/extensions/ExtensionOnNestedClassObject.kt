package foo

class A {
    class B {
        companion object
    }
}

fun A.B.Companion.foo() {}

fun some() {
    A.B.<caret>
}

// EXIST: foo
