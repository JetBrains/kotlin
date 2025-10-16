interface Foo {
    fun foo(hint: String = "X"): String
    fun default(s: String = "def"): String = "Default"
}

open class Bar {
    open fun bar(): String = "Bar"
}
