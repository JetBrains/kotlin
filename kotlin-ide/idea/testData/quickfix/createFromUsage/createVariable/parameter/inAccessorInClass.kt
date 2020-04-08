// "Create property 'foo' as constructor parameter" "true"

class A {
    val test: Int get() {
        return <caret>foo
    }
}