// PROBLEM: none

fun test() {
    try {
    } catch (_: Throwable) {
        <caret>`_`.printStackTrace()
    }
}