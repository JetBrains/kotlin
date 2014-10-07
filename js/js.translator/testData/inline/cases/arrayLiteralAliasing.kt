package foo

// CHECK_NOT_CALLED: moveTo

native fun Array<Int>.push(element: Int): Unit = noImpl

native fun Array<Int>.splice(index: Int, howMany: Int): Unit = noImpl

data class PairArray<T, R>(val fst: Array<T>, val snd: Array<R>)

inline fun moveTo(source: Array<Int>, sink: Array<Int>): PairArray<Int, Int> {
    val size = source.size
    for (i in 1..size) {
        val element = source[0]
        source.splice(0, 1)
        sink.push(element)
    }

    return PairArray(source, sink)
}

fun box(): String {
    val expected = PairArray<Int, Int>(array(), array(1,2,3,4))
    assertEquals(expected, moveTo(array(3, 4),  array(1, 2)))

    return "OK"
}