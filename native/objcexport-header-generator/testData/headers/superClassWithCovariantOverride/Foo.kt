abstract class Bar {
    abstract fun foo(): Any
}

class Foo : Bar() {
    override fun foo(): String = error("stub")
}
