package foo

class <caret>A {
    val a: A = A()
    val b: B = B()
    val c: C = C()
}

class B {
    val a: A = A()
    val b: B = B()
    val c: C = C()
}

class <caret>C {
    val a: A = A()
    val b: B = B()
    val c: C = C()
}