package foo

class Test() {
    var a : Int = 1
}

fun box() : Int {
    var a = Test()
    var b = Test()
    b.a = 100
    return (b.a - a.a)
}