// "Create enum constant 'A'" "false"
// ACTION: Create member property 'J.A'
// ACTION: Rename reference
// ERROR: Unresolved reference: A
fun foo() = J.<caret>A
