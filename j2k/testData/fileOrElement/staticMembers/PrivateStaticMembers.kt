class A {

    public fun foo() {
        privateStatic1()
        privateStatic2()
    }

    default object {
        private val s = "abc"

        private fun privateStatic1() {
        }

        private fun privateStatic2() {
        }
    }
}