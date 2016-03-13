package test

object Test {
    @JvmStatic fun main(args: Array<String>) {
        println()// Comment

        Test.foo()// Comment1
                .indexOf("s")// Comment2
    }

    fun foo(): String {
        return ""
    }
}