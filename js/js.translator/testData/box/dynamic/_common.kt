package foo

@native
var bar: dynamic = noImpl

@native
var arr: dynamic = noImpl

@native
var baz: dynamic = noImpl

object t {
    override fun toString() = "object t {}"
}

object n {
    fun valueOf() = 42
}
