// "Create annotation 'foo'" "false"
// ERROR: Unresolved reference: foo
// ACTION: Make private
// ACTION: Make internal

@J.<caret>foo(1, "2") fun test() {

}