// "Create property 'foo' as constructor parameter" "false"
// ACTION: Create local variable 'foo'
// ACTION: Create parameter 'foo'
// ACTION: Create property 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

class A {
    object B {
        fun test(): Int {
            return <caret>foo
        }
    }
}
