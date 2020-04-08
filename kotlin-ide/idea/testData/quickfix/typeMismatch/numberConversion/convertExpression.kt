// "Convert expression to 'Int'" "true"
// WITH_RUNTIME
fun foo() {
    bar("1".toLong()<caret>)
}

fun bar(l: Int) {
}