import java.util.ArrayList  

internal class A {
    fun createCollection(): MutableCollection<String?> {
        return ArrayList()
    }

    fun foo(): Collection<String?> {
        val collection = createCollection()
        collection.add("a")
        return collection
    }
}