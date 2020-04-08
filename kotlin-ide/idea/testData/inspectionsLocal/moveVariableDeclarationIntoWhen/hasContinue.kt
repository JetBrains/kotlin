// PROBLEM: none
fun test() {
    for (i in 1..10) {
        val <caret>some = foo(i) ?: continue
        when (some) {
            "some" -> some
            else -> ""
        }
    }
}

fun foo(i: Int): String? = ""