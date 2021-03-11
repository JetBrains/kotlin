// PROBLEM: none
// WITH_RUNTIME

fun foo(a: List<String>, b: List<Int>) {
    a.forEach {
        val <caret>a2 = it
        b.forEach {
            println(a2.length)
        }
    }
}