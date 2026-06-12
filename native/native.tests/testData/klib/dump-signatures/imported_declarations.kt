// MODULE: foo
package foo

private fun privateFun(): CharSequence = "hello"
fun publicFun() = privateFun()

private val privateVal: Appendable get() = StringBuilder()
val publicVal get() = privateVal

private class PrivateClass(val length: Int = privateVal.toString().length)
class PublicClass(val length: Int = privateFun().length)

// MODULE: bar
package bar

private fun privateFun(): Set<Int> = hashSetOf(1, 2, 3)
fun publicFun() = privateFun()

private val privateVal: List<String> get() = mutableListOf("foo", "bar")
val publicVal get() = privateVal

private class PrivateClass(val collection: Collection<*> = privateFun())
class PublicClass(val iterator: Iterator<*> = privateVal.iterator())

// MODULE: main(foo, bar)
package imported

private fun consume(anything: Any?) {
    anything.toString()
}

fun test() {
    consume(foo.publicFun())
    consume(bar.publicVal)
    consume(foo.PublicClass())
    consume(Any() as Map.Entry<Any, Any>) // accessing nested classifier kotlin/collections/Map.Entry without accessing it's parent class kotlin/collections/Map
}
