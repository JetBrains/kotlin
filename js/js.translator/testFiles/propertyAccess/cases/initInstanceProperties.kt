package foo

class Test() {
    val a: Int = 100
    var b: Int = a
    val c: Int = a + b
}

fun box(): Boolean {
    val test = Test()
    return (100 == test.a && 100 == test.b && 200 == test.c)
}
