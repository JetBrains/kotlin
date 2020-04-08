// WITH_RUNTIME

fun foo(t: Triple<String, Int, Boolean>) {
    val (_, _, <caret>_) = t
}