interface Foo {
    fun <caret>bar(s: String)
}

class Baz: Foo {
    override fun bar(s: String) {

    }
}