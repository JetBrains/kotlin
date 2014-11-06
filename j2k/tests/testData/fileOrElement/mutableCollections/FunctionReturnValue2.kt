import java.util.*

class A {
    private val collection: MutableCollection<String>

    {
        collection = createCollection()
    }

    fun createCollection(): MutableCollection<String> {
        return ArrayList()
    }

    public fun foo() {
        collection.add("1")
    }

    public fun getCollection(): Collection<String> {
        return collection
    }
}
