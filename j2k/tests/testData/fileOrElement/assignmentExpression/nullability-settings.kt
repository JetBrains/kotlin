// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
import java.util.HashSet

class Foo {
    fun foo(o: HashSet<Any?>?) {
        val o2: HashSet<Any?>? = o
        var foo: Int = 0
        foo = o2!!.size()
    }
}