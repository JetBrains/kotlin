class A {
    private class B {
        private class D

        private class <caret>C {
            private val d = D()
        }
    }
}