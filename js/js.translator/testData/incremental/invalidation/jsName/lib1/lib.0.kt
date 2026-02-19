interface Foo {
    fun foo(): String
    fun default(): String = "Default"
}

open class Bar {
    open fun bar(): String = "Bar"
}