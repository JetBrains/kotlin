class C {
    <caret>@Deprecated("")
    val foo: String
        get() {
            return bar()
        }

    fun bar(): String = ""
}
