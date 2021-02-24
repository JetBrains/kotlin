// !FORCE_NOT_NULL_TYPES: false
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
import java.util.HashSet

internal class Foo {
    fun foo(o: HashSet<*>?) {
        val o2: HashSet<*>? = o
        var foo: Int = 0
        foo = o2!!.size
    }
}