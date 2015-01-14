class C {
    public fun foo(s: String?) {
    }
}

class D {
    fun bar(c: C) {
        c.foo(null)
    }
}