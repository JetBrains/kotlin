class KClient {
    fun client() {
        class <caret>KA {
            val name = "A"
            fun foo(s: String): String = "A: $s"
        }

        val bar: String = KA().name

        fun bar() {
            fun localFun() = KA()

            KA()
        }

        object KClientObj {
            val a = KA()
        }
    }
}