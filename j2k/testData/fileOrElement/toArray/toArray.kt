import java.util.Arrays

class Foo {
    fun m(): Array<Any> {
        return Arrays.asList("a", "b").toTypedArray()
    }
}