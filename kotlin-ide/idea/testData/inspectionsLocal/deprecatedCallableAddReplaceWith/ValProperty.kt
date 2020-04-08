class C {
    <caret>@Deprecated("")
    val foo: String
        get() = bar()

    fun bar(): String = ""
}
