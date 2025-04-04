class Context

context(Context)
fun topLevelFoo() = Unit

class Bar {
    context(Context)
    fun memberFoo() = Unit
}