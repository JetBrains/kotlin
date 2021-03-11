// "Remove parameter 'x'" "true"
fun <X> foo(<caret>x: X) where X : Number {}

fun test() {
    foo(1)
}