fun foo(p: Int){}

fun f() {
    f<caret>
    (a + b).x()
}

// ELEMENT: foo
