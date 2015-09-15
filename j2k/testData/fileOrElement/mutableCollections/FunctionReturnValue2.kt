import java.util.*

internal class A internal constructor() {
    private val collection: MutableCollection<String>

    init {
        collection = createCollection()
    }

    internal fun createCollection(): MutableCollection<String> {
        return ArrayList()
    }

    fun foo() {
        collection.add("1")
    }

    fun getCollection(): Collection<String> {
        return collection
    }
}
