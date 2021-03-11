// WITH_RUNTIME
fun foo() {
    val x = "abcd"

    x.forEach<caret> { it.equals('a') }
}