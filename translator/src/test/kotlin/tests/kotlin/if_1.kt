fun if_test_1(x: Int): Int {
    var a = 0
    if (x > 5) {
        a = 10
    } else {

    }
    return a
}

class MyClass2(i: Int)

fun if_test_null(x: Int): Int {

    val y: MyClass2? = null
    if (y == null) {
        return 1
    } else {
        return 0
    }
}

fun if_test_null_2(x: Int): Int {
    val y: MyClass2? = MyClass2(1)

    if (y == null) {
        return 1
    } else {
        return 0
    }
}