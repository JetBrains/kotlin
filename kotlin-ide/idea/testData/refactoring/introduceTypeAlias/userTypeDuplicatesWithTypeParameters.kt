// NAME: S
class A<X, Y>

// SIBLING:
fun foo() {
    class B<T>

    val t1: <caret>A<B<Int>, A<B<Int>, Boolean>>
    val t2: A<B<Int>, A<B<Int>, Boolean>>
    val t3: A<String, A<String, Boolean>>
    val t4: A<Int, A<String, Boolean>>
    val t5: A<(Int) -> Int, A<() -> Int, Boolean>>
}