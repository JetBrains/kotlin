// "Terminate preceding call with semicolon" "true"

fun foo() {}

fun test() {
    foo()/*
        block
        comment
    */
    // comment
    {}<caret>
}