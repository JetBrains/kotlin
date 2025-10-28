@file:OptIn(kotlin.ExperimentalStdlibApi::class)

interface Foo {
    @JsSymbol("toPrimitive")
    fun foo(hint: String = "X"): String
    @JsSymbol("match")
    fun default(s: String = "def"): String = "Default"
}

open class Bar {
    @JsSymbol("iterator")
    open fun bar(): String = "Bar"
}
