class Foo {
    inner class Inner {}

    class Nested {
        operator fun plus(other: Int): Nested = this
    }
}

interface MyInterface

fun Foo.ext() {}

inline fun foo() {}

