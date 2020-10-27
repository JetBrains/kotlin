interface I {
    fun foo(): Int
}

class A : I {
    override fun foo() = 42
}

fun main(args: Array<String>) {
    lateinit var a: I
    if (args.size == 0)
        a = A()
    println(a.foo())
}