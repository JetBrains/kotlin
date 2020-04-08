interface X {
    infix fun infix(p: Int): X
}

fun foo(x: X) {
    x.in<caret>fix(1)
}