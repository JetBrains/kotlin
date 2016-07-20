
class Outer() {

    class Nested(var i: Int) {
    }
}

fun nested_test_1(k: Int): Int {
    val j = Outer.Nested(k - 1)
    j.i = k

    return j.i
}