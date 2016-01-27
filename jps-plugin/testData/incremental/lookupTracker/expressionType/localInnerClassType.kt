package foo

/*p:foo*/fun bar() {
    class A {
        inner class B
    }

    val b = A().B()
}