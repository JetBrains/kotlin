package foo

abstract class B {
    abstract fun foo(param: String): String
}

class A : B() {
    // must be here - before open fun foo(String)
    private fun foo(param: Int) = "foo(Int)"

    fun foo() = "foo()"

    override fun foo(param: String) = "foo(String)"
}

fun box(): Boolean {
    return A().foo("OK") == "foo(String)"
}