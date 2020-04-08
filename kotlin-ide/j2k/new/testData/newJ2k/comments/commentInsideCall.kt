package test

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        println() // Comment
        foo() // Comment1
                // Comment2
                .indexOf("s")
    }

    fun foo(): String {
        return ""
    }
}