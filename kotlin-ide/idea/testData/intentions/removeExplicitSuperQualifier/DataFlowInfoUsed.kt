open class B {
    open fun foo(p: String){}

    fun foo(p: Int){}
}

interface I {
    fun foo(p: String)
}

class A : B(), I {
    fun foo(p: Any) {
        if (p is Int) {
            super<B><caret>.foo(p)
        }
    }
}