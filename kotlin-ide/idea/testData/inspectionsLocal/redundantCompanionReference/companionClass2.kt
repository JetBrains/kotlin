class Test {
    fun test() {
        Companion.<caret>Companion.foo
    }
}
class Companion {
    companion object {
        val foo = ""
    }
}