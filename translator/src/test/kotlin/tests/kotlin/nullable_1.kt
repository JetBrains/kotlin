
class MyAwesomeClass(var i: Int)

fun nullable_test(i: Int): Int {
    var x: MyAwesomeClass? = null
    x = MyAwesomeClass(i)
    return x.i
}

fun nullable_test_2(i: Int): Int {
    var x: MyAwesomeClass? = MyAwesomeClass(i)
    x = null
    x = MyAwesomeClass(i)

    return x.i + 1
}