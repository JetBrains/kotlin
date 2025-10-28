interface Foo {
    @JsSymbol("toPrimitive")
    fun foo(hint: String = "X"): String
    @JsSymbol("match")
    fun default(s: String = "def"): String = "Default"
}

open class Bar {
    open fun bar(): String = "Bar"
}
