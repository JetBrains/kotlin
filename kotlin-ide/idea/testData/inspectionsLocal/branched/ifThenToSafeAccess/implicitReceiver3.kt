class Foo

class Bar {
    fun Foo?.test() {
        <caret>if (this@Bar != null) {
            bar()
        }
    }
    fun bar() {}
}