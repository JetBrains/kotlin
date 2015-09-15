internal class A {
    class Nested {
        internal fun foo() {
            privateStatic1()
        }
    }

    internal fun bar() {
        privateStatic2()
    }

    companion object {

        private fun privateStatic1() {
        }

        private fun privateStatic2() {
        }
    }
}