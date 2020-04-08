// "Add non-null asserted (!!) call" "true"
// WITH_RUNTIME

fun foo(a: List<String>?) {
    "x" <caret>in a
}