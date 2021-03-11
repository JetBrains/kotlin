// "Create property 'foo' as constructor parameter" "true"

class A<T> {
    val test: T get() {
        return <caret>foo
    }
}