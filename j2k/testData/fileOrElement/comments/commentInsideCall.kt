package test

object Test {
    @JvmStatic fun main(args: Array<String>) {
        println()// Comment

        Test
                // Comment1
                .foo()
                // Comment2
                .indexOf("s")
    }

    fun foo(): String {
        return ""
    }
}