class A {

    public fun foo() {
        privateStatic1()
        privateStatic2()
    }

    class object {
        private val s = "abc"

        private fun privateStatic1() {
        }
        private fun privateStatic2() {
        }
    }
}