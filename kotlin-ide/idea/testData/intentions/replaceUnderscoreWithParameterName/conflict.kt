// WITH_RUNTIME

fun foo(pair: Pair<Int, Int>) {
    val (<caret>_, _) = pair
    val first = 42
}