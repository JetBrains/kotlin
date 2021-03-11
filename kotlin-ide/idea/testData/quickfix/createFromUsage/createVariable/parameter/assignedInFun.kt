// "Create parameter 'foo'" "false"
// ACTION: Create local variable 'foo'
// ACTION: Create property 'foo'
// ACTION: Rename reference
// ACTION: Convert assignment to assignment expression
// ERROR: Unresolved reference: foo

fun test(n: Int) {
    <caret>foo = n + 1
}