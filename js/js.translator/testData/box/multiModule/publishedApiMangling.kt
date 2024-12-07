// KJS_WITH_FULL_RUNTIME
class A {
    @PublishedApi
    internal fun published(x: String) = "${x}K"
}

@PublishedApi
internal fun publishedTopLevel(x: String) = "${x}K"

interface I {
    fun test(): String
}

@PublishedApi
internal class B(val x: String) : I {
    override fun test() = x + "K"
}

fun box(): String = "OK"
