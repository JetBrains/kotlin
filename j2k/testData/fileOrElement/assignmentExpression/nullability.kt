import java.util.HashSet

class Foo {
    fun foo(o: HashSet<Any>) {
        val o2 = o
        var foo = 0
        foo = o2.size()
    }
}