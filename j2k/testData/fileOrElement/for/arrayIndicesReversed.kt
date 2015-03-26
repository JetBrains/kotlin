public class A {
    fun foo(array: Array<String>) {
        for (i in array.indices.reversed()) {
            System.out.println(i)
        }
    }
}
