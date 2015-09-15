import java.util.*

internal class A {
    internal fun foo(collection: MutableCollection<String>) {
        bar(collection)
    }

    internal fun bar(collection: MutableCollection<String>) {
        if (collection.size() < 5) {
            foo(collection)
        } else {
            collection.add("a")
        }
    }
}
