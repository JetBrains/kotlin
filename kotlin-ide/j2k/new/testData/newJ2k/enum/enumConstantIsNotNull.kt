internal enum class E {
    FOO
}

internal object A {
    @JvmStatic
    fun main(args: Array<String>) {
        println(E.FOO.toString())
    }
}