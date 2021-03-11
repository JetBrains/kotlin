class Foo : Bar() {
    fun test(): String {
        return <caret>Companion.bar
    }

    companion object {
        val bar: String = "bar"
    }
}