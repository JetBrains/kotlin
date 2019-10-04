// ERROR: Unresolved reference: LinkedList
import java.util.ArrayList

class Lists {
    fun test() {
        val xs: MutableList<Any?> = ArrayList()
        val ys: MutableList<Any?> = LinkedList<Any>()
        val zs = ArrayList<Any?>()
        xs.add(null)
        ys.add(null)
        xs.clear()
        ys.clear()
        zs.add(null)
    }
}