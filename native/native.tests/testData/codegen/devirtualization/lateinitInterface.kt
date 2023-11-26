// OUTPUT_DATA_FILE: lateinitInterface.out
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

fun box(): String {
    main(emptyArray<String>())

    return "OK"
}
