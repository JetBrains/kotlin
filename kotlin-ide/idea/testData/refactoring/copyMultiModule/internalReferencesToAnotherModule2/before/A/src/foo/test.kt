package foo

internal class <caret>A {
    val a: A = A()
    val b: B = B()
}

internal class B {
    val a: A = A()
    val b: B = B()
}