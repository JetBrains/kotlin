package test

private interface A {
    fun foo() = 0
    fun bar(): String
}

class B: A {
    override fun bar() = "test${foo()}"
}