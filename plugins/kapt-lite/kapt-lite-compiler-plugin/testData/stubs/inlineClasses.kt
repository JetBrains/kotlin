package inlineClasses

inline class Foo(val value: Int) {
    fun foo() {}
}

class Bar {
    fun bar(foo: Foo) {}

    val x: Foo?
        get() = null

    var y: Foo? = null
}

class Baz(val value: Foo)

fun Foo.ext() = 0

val Foo.prop: String
    get() = ""