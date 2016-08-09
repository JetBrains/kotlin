class this_test_1_class {
    var data: Int = 5
    fun f(x: Int) {
        data = x + 5
    }
}

fun this_test_1(arg: Int): Int {
    val instance = this_test_1_class()
    instance.f(arg)
    return instance.data
}