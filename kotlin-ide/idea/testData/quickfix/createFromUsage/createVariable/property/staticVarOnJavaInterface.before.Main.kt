// "Create property 'foo'" "false"
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test() {
    J.<caret>foo = 1
}
