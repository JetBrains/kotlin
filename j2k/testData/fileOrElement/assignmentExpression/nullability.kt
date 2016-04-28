import java.util.HashSet

internal class Foo {
    fun foo(o: HashSet<*>) {
        val o2 = o
        var foo = 0
        foo = o2.size
    }
}