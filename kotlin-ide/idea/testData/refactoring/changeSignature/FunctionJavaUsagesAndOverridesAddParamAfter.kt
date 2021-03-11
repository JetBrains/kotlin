open class A {
    open fun foo(n: Int, s: String, o: Any?): String = ""
}

class B: A() {
    override fun foo(n: Int, s: String, o: Any?): String = ""
}

fun test() {
    A().foo(1, "abc", "def")
    B().foo(2, "abc", "def")
    X().foo(3, "abc", "def")
    Y().foo(4, "abc", "def")
}