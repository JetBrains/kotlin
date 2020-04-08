// PROBLEM: Leaking 'this' in constructor of enum class Foo (with overridable members)
// FIX: none
enum class Foo {
    ONE {
        override val x = 1
    },
    TWO {
        override val x = 2
    };

    abstract val x: Int
    val double = double(<caret>this)
}

fun double(foo: Foo) = foo.x * foo.x

fun test() {
    Foo.ONE.double
    Foo.TWO.double
}