// FIR_IDENTICAL

package callables

class Foo(val list: List<*>)
private class Bar(val collection: Collection<*>)

fun funWithoutParameters() = 42
fun funWith1Parameter(map: Map<*, *>) = map.entries
fun funWith2Parameters(pair: Pair<*, *>, foo: Foo) = pair.hashCode() + foo.list.size
private fun funWithPrivateParameter(bar: Bar) = bar.collection.iterator()

fun funWithLocalMembers() {
    class LocalClass
    fun localFun() {
        LocalClass().toString()
    }
    localFun()
}


