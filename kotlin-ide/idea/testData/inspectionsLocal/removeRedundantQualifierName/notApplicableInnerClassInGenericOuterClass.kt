// PROBLEM: none
class Outer<T>(val inner: <caret>Outer<T>.Inner? = null) {
    inner class Inner
}