package lib1

interface A {
    private fun foo() = "A.foo"

    fun bar() = foo()
}