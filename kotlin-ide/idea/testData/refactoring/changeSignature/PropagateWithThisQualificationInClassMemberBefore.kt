fun <caret>foo(): Int = 1

class A(val n: Int) {
    fun bar(): Int {
        return foo() + n
    }
}
