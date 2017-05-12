class A(val x: Int) {
    inner class B {
        fun foo() {
            println(x)
        }
    }
}

// LINES: 1 2 4