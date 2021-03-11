// NAME: S
class A<X, Y>

// SIBLING:
fun foo() {
    val t1: <caret>A<Int, Boolean>
    val t2: A<Int, Boolean>
    val t3: A<Boolean, Int>
}