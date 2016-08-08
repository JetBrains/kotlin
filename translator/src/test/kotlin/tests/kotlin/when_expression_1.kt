fun when_expression_1(x: Int): Int {
    val z = when (x) {
        21234 -> 20
        55555 -> 50
        else -> 100
    }
    return z
}

fun when_expression_2 (value: Byte): Int {
    val result =  when (value) {
        0.toByte() -> 100
        1.toByte() -> 101
        2.toByte() -> 102
        3.toByte() -> 103
        4.toByte() -> 104
        5.toByte() -> 105
        else -> 199
    }

    return result
}