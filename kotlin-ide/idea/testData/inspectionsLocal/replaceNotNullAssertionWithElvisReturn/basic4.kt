class Foo {
    fun baz(): Int? = null
}

fun test(foo: Foo): Int? {
    foo.baz()!!<caret>
    return 0
}