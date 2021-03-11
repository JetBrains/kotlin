// WITH_RUNTIME

fun test(s: Sequence<Int>) {
    val foo = s<caret>.orEmpty()
}