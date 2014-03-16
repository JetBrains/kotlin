package foo

class Test() {
    var a: Int = 5
        set(b: Int) {
            $a = 3
        }
}

fun box(): Boolean {
    var test = Test()
    test.a = 5
    return (test.a == 3)
}