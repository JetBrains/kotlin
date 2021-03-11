// "Create member function 'Foo.iterator'" "true"
class Foo<T>
fun foo() {
    for (i: Int in Foo<caret><Int>()) { }
}
