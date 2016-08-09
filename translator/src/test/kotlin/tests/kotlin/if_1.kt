fun if_test_1(x: Int): Int {
    var a = 0
    if (x > 5) {
        a = 10
    } else {

    }
    return a
}

class MyClass_if_1()

fun if_test_null(x: Int): Int {
    val y: MyClass_if_1? = null
    if (y == null) {
        return 1
    } else {
        return 0
    }
}

fun if_test_null_2(x: Int): Int {
    val y: MyClass_if_1? = MyClass_if_1()

    if (y == null) {
        return 1
    } else {
        return 0
    }
}