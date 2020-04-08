class A {
    private val a = B()

    private class <caret>B {
        private val c = C()
    }

    private class C()
}