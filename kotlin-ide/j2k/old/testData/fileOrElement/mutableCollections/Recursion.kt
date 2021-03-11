import java.util.*

internal class A {
    fun foo(collection: MutableCollection<String>) {
        bar(collection)
    }

    fun bar(collection: MutableCollection<String>) {
        if (collection.size < 5) {
            foo(collection)
        } else {
            collection.add("a")
        }
    }
}
