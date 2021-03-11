// NAME: C
class A<X, Y>

// SIBLING:
fun foo() {
    class B<X, Y>

    val a: <caret>A<B<Int, String>, B<String, Int>>
}