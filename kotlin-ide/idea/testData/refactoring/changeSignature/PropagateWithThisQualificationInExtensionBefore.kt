fun <caret>foo(): Int = 1

class A(val n: Int)

fun A.bar(): Int {
    return foo() + n
}
