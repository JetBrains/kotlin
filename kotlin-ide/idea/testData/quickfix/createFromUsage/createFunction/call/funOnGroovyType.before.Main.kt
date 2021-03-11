// "Create member function 'foo'" "false"
// ACTION: Create extension function 'A.foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test(): Int {
    return A().<caret>foo()
}