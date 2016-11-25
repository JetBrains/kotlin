package foo

external var bar: dynamic = noImpl

external var arr: dynamic = noImpl

external var baz: dynamic = noImpl

object t {
    override fun toString() = "object t {}"
}

object n {
    fun valueOf() = 42
}
