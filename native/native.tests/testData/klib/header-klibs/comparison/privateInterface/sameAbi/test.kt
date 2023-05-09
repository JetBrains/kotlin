package test

private interface A {
    fun foo() = 42
    fun bar(): String
}

class B: A {
    override fun bar() = "test${foo()}"
}
