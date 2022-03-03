class A(val x: Int) {
    inner class B {
        fun foo() {
            println(x)
        }
    }
}

// LINES(JS):    1 1 2 2 3 5 4 4
// LINES(JS_IR):     2 2 *   4 4 * 1 1 * 1 1
