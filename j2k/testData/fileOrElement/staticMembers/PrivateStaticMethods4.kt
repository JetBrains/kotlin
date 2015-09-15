internal class A {
    inner class Inner {
        internal fun foo() {
            privateStatic1()
        }
    }

    internal fun bar() {
        privateStatic2()
    }

    private fun privateStatic1() {
    }

    private fun privateStatic2() {
    }
}