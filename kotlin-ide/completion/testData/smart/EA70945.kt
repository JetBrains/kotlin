class A {
    val foo: Int = 0
}

fun f() {
    A().foo(<caret>)
}

// NUMBER: 0
