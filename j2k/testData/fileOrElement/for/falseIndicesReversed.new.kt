class A {
    internal fun foo(collection: Collection<String?>) {
        for (i in collection.size downTo 0) {
            println(i)
        }
    }
}
