package kotlin

fun <T> assertArrayEquals(expected: Array<out T>, actual: Array<out T>, message: String? = null) {
    if (!arraysEqual(expected, actual)) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Unexpected array: expected = '$expected', actual = '$actual'$msg")
    }
}

private fun <T> arraysEqual(first: Array<out T>, second: Array<out T>): Boolean {
    if (first === second) return true
    if (first.size != second.size) return false
    for (index in 0..first.size - 1) {
        if (!equal(first[index], second[index])) return false
    }
    return true
}

private fun equal(first: Any?, second: Any?) =
    if (first is Array<*> && second is Array<*>) {
        arraysEqual(first, second)
    }
    else {
        first == second
    }