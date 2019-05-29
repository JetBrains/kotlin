package test

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        println()// Comment

        // Comment1
        foo()
                // Comment2
                .indexOf("s")
    }

    fun foo(): String {
        return ""
    }
}