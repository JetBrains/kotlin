import java.util.*

internal class A {
    internal fun createCollection(): MutableCollection<String> {
        return ArrayList()
    }

    internal fun foo(): Collection<String> {
        val collection = createCollection()
        collection.add("a")
        return collection
    }
}
