// WITH_RUNTIME
fun foo() {
    val x = 1..4

    x.<caret>forEach { a -> a }
}