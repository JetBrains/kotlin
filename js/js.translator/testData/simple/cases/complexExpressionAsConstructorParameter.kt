package foo

class Test(a: Int, b: Int) {
    val c = a
    val d = b
}

fun box(): Boolean {
    val test = Test(1 + 6 * 3, 10 % 2)
    return (test.c == 19) && (test.d == 0)
}