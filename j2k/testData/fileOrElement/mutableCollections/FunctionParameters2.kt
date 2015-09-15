import java.util.*

internal class A {
    internal fun foo(set: MutableSet<String>) {
        bar(set)
    }

    internal fun bar(collection: MutableCollection<String>) {
        collection.add("a")
    }
}
