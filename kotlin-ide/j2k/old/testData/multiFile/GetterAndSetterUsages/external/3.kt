fun foo1(a: AAA) {
    a.setX(a.getX() + 1)
}

fun foo2(a: AAA?) {
    a?.setX((a?.getX() ?: 0) + 1)
}

fun AAA.foo() {
    setX(getX() + 1)
}