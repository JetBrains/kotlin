internal class A {

    fun foo() {
        privateStatic1()
        privateStatic2()
    }

    companion object {
        private const val s = "abc"

        private fun privateStatic1() {}
        private fun privateStatic2() {}
    }
}