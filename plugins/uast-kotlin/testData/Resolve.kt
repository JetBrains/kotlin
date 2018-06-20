class A {
    fun foo() {}
    inline fun inlineFoo() {

    }
}

fun bar() {
    A().foo()
    A().inlineFoo()
    listOf(A()).forEach { println(it) } // inline from stdlib
    listOf("").joinToString() // not inline from stdlib
    listOf("").size // property from stdlib
    listOf("").last() // overloaded extension from stdlib
    mutableMapOf(1 to "1").entries.first().setValue("123") // call on nested method in stdlib
    val intRange = 0L..3L
    intRange.contains(2 as Int) // extension-fun with @JvmName("longRangeContains")
    IntRange(1, 2) // constructor from stdlib
}
