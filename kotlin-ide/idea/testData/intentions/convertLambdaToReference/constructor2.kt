class A {
    class B {}
}

fun foo(x: () -> A.B) {}

fun main() {
    foo <caret>{ A.B() }
}