// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
import java.util.BitSet

class Foo() {
    fun foo(o: BitSet?) {
        val o2: BitSet? = o
        var foo: Int = 0
        foo = o2!!.size()
    }
}