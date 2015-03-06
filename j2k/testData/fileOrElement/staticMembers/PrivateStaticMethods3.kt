class A {
    public class Nested {
        fun foo() {
            privateStatic1()
        }
    }

    fun bar() {
        privateStatic2()
    }

    default object {

        private fun privateStatic1() {
        }

        private fun privateStatic2() {
        }
    }
}