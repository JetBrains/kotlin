class C {
    public var s: String? = ""
}

class D {
    fun foo(c: C) {
        if (null == c.s) {
            println("null")
        }
    }
}