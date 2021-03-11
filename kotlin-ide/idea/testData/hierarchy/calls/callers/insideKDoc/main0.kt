open class K {
    public fun <caret>persist() {}
}

class Foo : K() {
    /**
     * [persist]
     */
    fun foo() {

    }
}
