// "Create property 'foo' as constructor parameter" "true"

class A {
    class B {
        fun test(): Int {
            return <caret>foo
        }
    }
}
