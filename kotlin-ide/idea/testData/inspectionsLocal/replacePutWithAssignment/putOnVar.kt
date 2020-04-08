// WITH_RUNTIME

fun foo() {
    var map = mutableMapOf(42 to "foo")
    map.<caret>put(60, "bar")
}