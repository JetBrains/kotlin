// "Create property 'foo' as constructor parameter" "true"
// ERROR: No value passed for parameter 'foo'

class A<T>(val n: T)

fun test() {
    A(1).<caret>foo = "1"
}
