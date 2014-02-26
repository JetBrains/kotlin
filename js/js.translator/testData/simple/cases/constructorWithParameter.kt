package foo

class Test(a: Int) {
    val b = a
}

fun box(): Boolean {
    var test = Test(1)
    return (test.b == 1)
}