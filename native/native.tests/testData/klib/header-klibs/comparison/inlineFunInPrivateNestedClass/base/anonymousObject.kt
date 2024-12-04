class A {
    private class B {
        inline fun test(crossinline s: () -> Unit) {
            object {
                fun run() {
                    s()
                }
            }.run()
        }
    }
}
