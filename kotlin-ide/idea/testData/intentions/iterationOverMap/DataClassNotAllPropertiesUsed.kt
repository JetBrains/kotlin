// WITH_RUNTIME

fun main(args: Array<String>) {
    val list = listOf(MyClass(1, 2, 3, 4))
    for (<caret>klass in list) {
        val a = klass.a
        val c = klass.c
        println("$a$c")
    }
}

data class MyClass(val a: Int, val b: Int, val c: Int, val d: Int)