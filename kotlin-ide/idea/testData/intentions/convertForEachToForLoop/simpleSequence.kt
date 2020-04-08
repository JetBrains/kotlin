// WITH_RUNTIME
fun foo() {
    val x = (1..4).asSequence()

    x.forEach<caret> { it.equals(1) }
}