// WITH_RUNTIME

class C {
    fun foo(s: String, s2: String = ""): String = "c"
    val bar = "C"
}

class D {
    fun baz(s: String): String = "d"
    val quux = "D"

    val x = C().<caret>let {
        it.foo(baz(it.foo(quux + it.bar)), baz(it.bar))
    }
}
