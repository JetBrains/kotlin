// NAME: C
class A<X, Y>

// SIBLING:
fun foo() {
    class B<X, Y>

    val a: <caret>A<B<B<Int, String>, String>, B<String, B<Int, String>>>
}