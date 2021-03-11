// "Create function 'bar'" "false"
// ACTION: Create annotation 'bar'
// ACTION: Make internal
// ACTION: Make private
// ACTION: Rename reference
// ACTION: Put arguments on separate lines
// ERROR: Unresolved reference: foo
// ERROR: Unresolved reference: bar

@foo(1, "2", <caret>bar("3", 4)) fun test() {

}