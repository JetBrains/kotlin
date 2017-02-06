fun foo(): Boolean {
    class Local
    fun bar() = Local()

    val baz = fun() {
        Local()
    }

    return bar() == Local()
}