namespace foo

class Test() {
    val a : Int = 1
}

fun box() : Boolean {
    var test = Test()
    return (test.a == 1)
}