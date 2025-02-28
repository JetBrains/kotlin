fun box(): String {
    return test1() +
            test2() +
            test3() +
            test4()
}

fun test1() = expectThrowableMessage {
    assert(listOf("Hello", "World")[1] == "Hello")
}

fun test2() = expectThrowableMessage {
    assert(listOf("Hello", "World").get(1) == "Hello")
}

fun test3() = expectThrowableMessage {
    val map = mapOf("key1" to 100, "key2" to 200)
    assert(map["key3"] == 300)
}

class MultiDimArray(val data: Array<Array<Int>>) {
    operator fun get(row: Int, col: Int): Int = data[row][col]
    override fun toString(): String = data.contentDeepToString()
}

fun test4() = expectThrowableMessage {
    val matrix = MultiDimArray(arrayOf(arrayOf(1, 2, 3)))
    assert(matrix[0, 1] == 99)
}

