open class Bar {
    companion object {
        class FromBarCompanion
    }
}

class Foo : Bar() {
    val a: <selection>Companion.FromBarCompanion? = null</selection>
}