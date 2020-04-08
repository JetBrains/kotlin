// "Create property 'foo'" "false"
// ERROR: Unresolved reference: foo
// ACTION: Create extension function 'Int.foo'
// ACTION: Replace infix call with ordinary call
// WITH_RUNTIME
fun refer() {
    1 <caret>foo 2
}