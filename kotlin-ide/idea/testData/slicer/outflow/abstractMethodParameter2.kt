// FLOW: OUT

interface I {
    fun foo(p: Any)
}

class C : I {
    override fun foo(p: Any) {
        println(p)
    }
}

fun bar(i: I, s: String) {
    i.foo(<caret>s)
}
