package test

private interface A {
    fun foo(): Int { if (true) return 1 else return 0 }
    fun bar(): String
}

class B: A {
    override fun bar() = "test${foo()}"
}