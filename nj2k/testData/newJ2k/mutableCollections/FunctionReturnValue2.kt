internal class A {
    private val collection: MutableCollection<String>
    fun createCollection(): MutableCollection<String> {
        return ArrayList()
    }

    fun foo() {
        collection.add("1")
    }

    fun getCollection(): Collection<String> {
        return collection
    }

    init {
        collection = createCollection()
    }
}