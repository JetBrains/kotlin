// PROBLEM: none
interface Inv<X>

class Outer<E> {
    inner class Inner

    class Nested : Inv<<caret>Outer<String>.Inner>
}
