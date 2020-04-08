// NAME: C
class B<X>

// SIBLING:
fun foo() {
    class A<X>

    val a: <caret>(B<A<Int>>, A<Int>, A<String>) -> A<Int>
}