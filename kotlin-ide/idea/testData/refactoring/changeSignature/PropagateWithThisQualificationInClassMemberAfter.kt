fun <caret>foo(n: Int): Int = 1

class A(val n: Int) {
    fun bar(n: Int): Int {
        return foo(n) + this.n
    }
}
