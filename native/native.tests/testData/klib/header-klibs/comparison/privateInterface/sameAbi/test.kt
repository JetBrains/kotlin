package test

private interface A {
    fun foo(): Int { if (true) return 0 else return 1 }
    fun bar(): String
}

class B: A {
    override fun bar() = "test${foo()}"
}
