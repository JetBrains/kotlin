// ERROR: Unresolved reference: LinkedList
import java.util.*

class Lists {
    fun test() {
        val xs: MutableList<Any?> = ArrayList()
        val ys: MutableList<Any?> = LinkedList<Any>()
        val zs: ArrayList<Any?> = ArrayList()
        xs.add(null)
        ys.add(null)
        xs.clear()
        ys.clear()
        zs.add(null)
    }
}