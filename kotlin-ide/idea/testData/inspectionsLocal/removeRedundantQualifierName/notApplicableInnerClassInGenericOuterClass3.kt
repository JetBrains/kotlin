// PROBLEM: none
class Outer<E> {
    inner class Inner

    class Nested {
        fun bar(x: <caret>Outer<String>.Inner) {}
    }
}