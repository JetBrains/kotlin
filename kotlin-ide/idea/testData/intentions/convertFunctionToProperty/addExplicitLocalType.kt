fun test() {
    class X

    class A(val n: Int) {
        fun <caret>foo() = X()
    }

    val t = A(1).foo()
}