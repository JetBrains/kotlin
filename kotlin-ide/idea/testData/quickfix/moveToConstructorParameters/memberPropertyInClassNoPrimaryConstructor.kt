// "Move to constructor parameters" "true"
open class A {
    <caret>val n: Int
}

class B : A()

fun test() {
    val a = A()
}