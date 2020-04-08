// "Create property 'foo'" "false"
// ACTION: Create parameter 'foo'
// ACTION: Create local variable 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test() {
    fun nestedTest(): Int {
        return <caret>foo
    }
}
