class A(val x: Int) {
    inner class B {
        fun foo() {
            println(x)
        }
    }
}

// LINES: 1 1 2 2 3 5 4 4