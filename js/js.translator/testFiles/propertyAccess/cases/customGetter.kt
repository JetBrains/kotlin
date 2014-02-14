package foo

class Test() {
    val a: Int
        get() {
            return 5;
        }
}

fun box(): Boolean {
    var test = Test()
    return (test.a == 5)
}