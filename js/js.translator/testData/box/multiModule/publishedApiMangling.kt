// EXPECTED_REACHABLE_NODES: 1108
// PROPERTY_WRITE_COUNT: name=publishedTopLevel_61zpoe$ count=1
// PROPERTY_WRITE_COUNT: name=published_61zpoe$ count=1
// PROPERTY_WRITE_COUNT: name=B count=1
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