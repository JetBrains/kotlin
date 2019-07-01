class A {
    internal fun foo(collection: Collection<String?>) {
        for (i in collection.indices.reversed()) {
            println(i)
        }
    }
}
