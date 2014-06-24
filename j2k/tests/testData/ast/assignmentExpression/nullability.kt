import java.util.BitSet

class Foo() {
    fun foo(o: BitSet) {
        val o2 = o
        var foo = 0
        foo = o2.size()
    }
}