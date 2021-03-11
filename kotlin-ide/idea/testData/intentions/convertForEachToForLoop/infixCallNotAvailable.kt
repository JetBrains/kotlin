// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo() {
    val x = 1..4

    <caret>x.forEach { a -> a }
}