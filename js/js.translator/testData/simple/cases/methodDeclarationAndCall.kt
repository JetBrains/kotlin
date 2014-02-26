package foo

class Test() {
    fun method(): Boolean {
        return true;
    }
}

fun box(): Boolean {
    var test = Test()
    return test.method()
}