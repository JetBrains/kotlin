package foo

trait Foo {
    fun f() : Boolean
}

object foo : Foo {
    override fun f() = true
}

fun box() : Boolean {
    return foo.f()
}