interface Foo {
    @JsName("_foo_")
    fun foo(): String
    fun default(): String = "Default"
}

open class Bar {
    open fun bar(): String = "Bar"
}