// "Create member function 'FooIterator.hasNext'" "true"
class FooIterator<T> {
    operator fun next(): Int {
        throw Exception("not implemented")
    }
}
class Foo<T> {
    operator fun iterator(): FooIterator<String> {
        throw Exception("not implemented")
    }
}
fun foo() {
    for (i: Int in Foo<caret><Int>()) { }
}
