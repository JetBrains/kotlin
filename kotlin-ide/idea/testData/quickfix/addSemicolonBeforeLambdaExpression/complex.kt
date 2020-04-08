// "Terminate preceding call with semicolon" "true"

fun foo() {}
fun String.withLambda(lambda: (String) -> Unit) {}

fun test {
    { { "test" } }.invoke()().toString().withLambda()
    // comment and formatting
    {} // correct trailing lambda
    /*
       block comment
    */
    { { { foo() }<caret> } }.invoke()().invoke()
}
