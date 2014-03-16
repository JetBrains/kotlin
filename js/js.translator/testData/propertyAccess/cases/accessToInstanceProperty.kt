package foo

class Test() {
    var a: Int = 100
}

fun box(): Boolean {
    var test = Test()
    test.a = 1
    return (1 == test.a)
}