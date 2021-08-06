private class A {
    inline fun test(crossinline s: () -> Unit) {
        object {
            fun run() {
                s()
            }
        }.run()
    }
}
