class Bar {
}

class Foo() {
    fun Bar.invoke(): Int = 1
}

fun main(args: Array<String>) {
    val f = Foo()
    println(<selection>Bar().f()</selection>)
    println(Bar().f())
}