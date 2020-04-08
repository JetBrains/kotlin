// "Create member function 'FooIterator.hasNext'" "true"
class FooIterator<T> {
    operator fun next(): T {
        throw Exception("not implemented")
    }
}
class Foo<T> {
    operator fun iterator(): FooIterator<T> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i in Foo<caret><Int>()) { }
}
