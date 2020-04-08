// "Create property 'foo' as constructor parameter" "true"

class A {
    fun test(n: Int) {
        <caret>foo = n + 1
    }
}
