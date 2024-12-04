class A(val x: Int) {
    inner class B {
        fun foo() {
            println(x)
        }
    }
}

// LINES(JS_IR): 2 2 2 2 3 4 4 1 1 1 1 1 1 1
