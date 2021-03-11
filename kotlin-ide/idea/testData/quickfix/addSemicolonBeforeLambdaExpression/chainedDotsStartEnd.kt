// "Terminate preceding call with semicolon" "true"

fun test() {
    "test".toString().toString().toString()
    {<caret>"test"}.invoke().toString().toString()
}
