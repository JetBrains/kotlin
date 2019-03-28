import java.util.*

internal class A {
    fun foo(set: MutableSet<String>) {
        bar(set)
    }

    fun bar(collection: MutableCollection<String>) {
        collection.add("a")
    }
}
