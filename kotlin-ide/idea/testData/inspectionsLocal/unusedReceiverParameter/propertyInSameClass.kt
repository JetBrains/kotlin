class Test(private val foo: Int) {
    val <caret>Test.baz: Int
        get() = foo + this.foo + this@baz.foo

    fun test(test: Test) {
        println(baz)
        println(test.baz)
        println(Test(3).baz)
    }
}

fun main(args: Array<String>) {
    Test(1).test(Test(2))
}

fun println(a: Any) {
    // kotlin.io.println(a)
}
