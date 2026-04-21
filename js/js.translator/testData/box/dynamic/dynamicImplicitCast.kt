// ISSUE: KT-85701
internal fun compareTo(a: dynamic, b: dynamic): Int = when (jsTypeOf(a)) {
    else -> compareToDoNotIntrinsicify(a, b)
}

private fun <T : Comparable<T>> compareToDoNotIntrinsicify(a: Comparable<T>, b: T) =
    a.compareTo(b)

fun box(): String {
    if (compareTo(1.0, 1.0) != 0) return "FAIL"
    return "OK"
}
