fun acceptLambda(f: () -> Unit) = f()

fun foo() {
    acceptLambda(<caret>fun(): Unit {})
}