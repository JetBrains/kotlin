// ERROR: Unresolved reference: LinkedList
// ERROR: Null can not be a value of a non-null type kotlin.Any
// ERROR: Null can not be a value of a non-null type kotlin.Any
import java.util.*

public class Lists {
    public fun test() {
        val xs = ArrayList<Any>()
        val ys = LinkedList<Any>()
        val zs = ArrayList<Any>()
        xs.add(null)
        ys.add(null)
        xs.clear()
        ys.clear()
        zs.add(null)
    }
}