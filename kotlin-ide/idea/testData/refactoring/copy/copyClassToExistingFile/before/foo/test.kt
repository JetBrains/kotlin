package foo

class <caret>A {
    val a: A = A()
    val b: B = B()
}

class B {
    val a: A = A()
    val b: B = B()
}
