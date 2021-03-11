class Foo {
    fun getBar(): String {
        return <caret>Companion.bar
    }

    companion object {
        val bar: String = "bar"
    }
}