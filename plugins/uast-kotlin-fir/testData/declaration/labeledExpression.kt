interface I {
    fun foo(): Int
}

abstract class Base {
    fun foo(): Int {
        return 42
    }
}

class Foo : I, Base() {
    val p: String = "42"

    fun bar(other: I): Int {
        with(other) {
            return super@Foo.foo()
        }
    }

    fun baz(other: I): String {
        with(other) {
            return this@Foo.p
        }
    }
}

