// ERROR: Unresolved reference: LinkedList
// ERROR: Null can not be a value of a non-null type Any
// ERROR: Null can not be a value of a non-null type Any
import java.util.ArrayList

class Lists {
    fun test() {
        val xs: MutableList<Any?> = ArrayList()
        val ys: MutableList<Any?> = LinkedList<Any?>()
        val zs: ArrayList<Any?> = ArrayList()
        xs.add(null)
        ys.add(null)
        xs.clear()
        ys.clear()
        zs.add(null)
    }
}