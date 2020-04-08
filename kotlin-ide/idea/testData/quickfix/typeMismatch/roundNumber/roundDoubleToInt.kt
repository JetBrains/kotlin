// "Round using roundToInt()" "true"
// WITH_RUNTIME
fun test(d: Double) {
    foo(d<caret>)
}

fun foo(x: Int) {}