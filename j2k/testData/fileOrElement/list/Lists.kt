// ERROR: Unresolved reference: LinkedList
// ERROR: Null can not be a value of a non-null type Any
// ERROR: Null can not be a value of a non-null type Any
import java.util.*

class Lists {
    fun test() {
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