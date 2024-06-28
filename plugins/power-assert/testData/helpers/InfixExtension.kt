package infix.extension

infix fun <V> V.mustEqual(expected: V): Unit = assert(this == expected)
fun <V> V.mustEqual(expected: V, message: () -> String): Unit =
    assert(this == expected, message)
