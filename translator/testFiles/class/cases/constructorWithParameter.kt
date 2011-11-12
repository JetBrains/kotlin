namespace foo

class Test(var a : Int) {
}

fun box() : Boolean {
    var test = Test(1)
    return (test.a == 1)
}