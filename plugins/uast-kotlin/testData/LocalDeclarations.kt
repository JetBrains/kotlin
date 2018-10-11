fun foo(): Boolean {
    class Local
    fun bar() = Local()

    val baz = fun() {
        Local()
    }

    fun Int.someLocalFun(text: String) = 42

    object LocalObject

    return bar() == Local()
}