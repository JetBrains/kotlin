package foo

// CHECK_NOT_CALLED: moveTo

@native fun Array<Int>.push(element: Int): Unit = noImpl

@native fun Array<Int>.splice(index: Int, howMany: Int): Unit = noImpl

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
    val expected = PairArray<Int, Int>(arrayOf(), arrayOf(1,2,3,4))
    assertTrue(expected.deepEquals(moveTo(arrayOf(3, 4),  arrayOf(1, 2))))

    return "OK"
}

fun <T, R> PairArray<T, R>.deepEquals(other: PairArray<T, R>): Boolean {
    return fst.asList() == other.fst.asList() && snd.asList() == other.snd.asList()
}