// ERROR: Type mismatch: inferred type is Any but String was expected
// ERROR: Type mismatch: inferred type is Any but String was expected
// ERROR: Type mismatch: inferred type is Array<String> but Array<Any> was expected
import java.util.Arrays

class Foo {
    fun test() {
        val list: List<String> = Arrays.asList("a", "b")
        val array1: Array<Any?> = list.toTypedArray()
        val array2: Array<Any> = list.toTypedArray<String>()
    }
}