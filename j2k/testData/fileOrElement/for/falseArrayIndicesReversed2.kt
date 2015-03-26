public class A {
    fun foo(array: Array<String>) {
        for (i in array.size() - 2 downTo 0) {
            System.out.println(i)
        }
    }
}
