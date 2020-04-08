// "Convert supertype to '(String, T) -> Unit'" "true"

class Foo<T> : <caret>String.(T) -> Unit {
    override fun invoke(p1: String, p2: T) {
    }
}