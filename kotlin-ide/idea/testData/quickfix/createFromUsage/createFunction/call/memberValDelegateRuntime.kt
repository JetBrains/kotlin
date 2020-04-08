// "Create function 'foo'" "true"

class A<T>(val t: T) {
    val x: A<Int> by <caret>foo(t, "")
}
