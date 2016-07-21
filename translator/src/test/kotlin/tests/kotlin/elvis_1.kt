
class MyClass(val i: Int)

fun elvis_test_1(x: Int): Int {
    var z: MyClass? = null

    if (x > 1) {
        z = MyClass(1)
    } else {
    }

    val y = z ?: return 0
    return 1
}


