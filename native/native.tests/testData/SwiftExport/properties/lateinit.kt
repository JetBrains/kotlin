class Foo(val value: Int) {
    override fun equals(other: Any?): Boolean {
        return other is Foo && other.value == value
    }
}

// Workaround absence of methods.
fun compare(a: Foo, b: Foo): Boolean {
    return a == b
}

lateinit var lateinitProperty: Foo