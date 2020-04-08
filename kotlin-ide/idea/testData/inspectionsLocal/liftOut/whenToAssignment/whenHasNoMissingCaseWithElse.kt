enum class TestEnum{
    A, B, C
}

fun test(e: TestEnum): Int {
    var res: Int = 0

    <caret>when (e) {
        TestEnum.A -> res = 1
        TestEnum.B -> res = 2
        else -> res = 3
    }

    return res
}