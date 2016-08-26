
class elvis_1_MyClass(val i: Int)

fun elvis_test_1(x: Int): Int {
    var z: elvis_1_MyClass? = null

    if (x > 1) {
        z = elvis_1_MyClass(1)
    } else {
    }

    z ?: return 0
    return 1
}

fun elvis_test_2(): Int {
    val i: elvis_1_MyClass? = null
    i ?: return 1
    return 0
}
