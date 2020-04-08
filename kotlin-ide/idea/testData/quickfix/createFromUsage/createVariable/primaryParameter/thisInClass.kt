// "Create property 'foo' as constructor parameter" "true"

class A<T>(val n: T) {
    fun test(): A<Int> {
        return this.<caret>foo
    }
}
