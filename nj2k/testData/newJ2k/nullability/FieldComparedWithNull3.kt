internal class C {
    var s: String? = ""
}

internal class D {
    fun foo(c: C) {
        if (null == c.s) {
            println("null")
        }
    }
}