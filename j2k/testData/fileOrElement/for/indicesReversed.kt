public class A {
    fun foo(collection: Collection<String>) {
        for (i in collection.indices.reversed()) {
            System.out.println(i)
        }
    }
}
