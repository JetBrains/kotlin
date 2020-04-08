// "Round using roundToInt()" "true"
// WITH_RUNTIME
fun test(f: Float) {
    foo(f<caret>)
}

fun foo(x: Int) {}