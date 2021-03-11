// PROBLEM: none
fun test() {
    for (i in 1..10) {
        val <caret>some = foo(i) ?: break
        when (some) {
            "some" -> some
            else -> ""
        }
    }
}

fun foo(i: Int): String? = ""