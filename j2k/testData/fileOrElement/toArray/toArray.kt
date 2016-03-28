import java.util.Arrays

class Foo {
    fun test() {
        val list = Arrays.asList("a", "b")
        val array1 = list.toTypedArray()
        val array2 = list.toTypedArray()
    }
}