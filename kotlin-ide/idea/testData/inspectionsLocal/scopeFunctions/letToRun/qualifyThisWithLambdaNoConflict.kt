// WITH_RUNTIME

class C {
    fun foo() = "c"
    val bar = "C"
}

class D {
    fun baz() = "d"
    val quux = "D"

}

val d = D()
val x = d.apply {
    C().<caret>let {
        baz() + it.foo() + quux + it.bar
    }
}
