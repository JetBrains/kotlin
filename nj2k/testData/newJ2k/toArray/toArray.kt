import java.util.Arrays

class Foo {
    fun test() {
        val list: List<String> = Arrays.asList("a", "b")
        val array1: Array<Any> = list.toTypedArray()
        val array2: Array<Any> = list.toTypedArray<String>()
    }
}