// "Create property 'foo'" "false"
// ACTION: Create abstract property 'foo'
// ACTION: Create local variable 'foo'
// ACTION: Create object 'foo'
// ACTION: Create parameter 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

interface IF1 {
    fun af2() = <caret>foo
}