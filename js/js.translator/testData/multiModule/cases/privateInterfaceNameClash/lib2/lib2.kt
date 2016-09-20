package lib2

interface B {
    private fun foo() = "B.foo"

    fun bar() = foo()
}