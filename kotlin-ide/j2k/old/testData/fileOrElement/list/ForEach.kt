// ERROR: Unresolved reference: LinkedList
import java.util.*

class ForEach {
    fun test() {
        val xs = ArrayList<Any>()
        val ys = LinkedList<Any>()
        for (x in xs) {
            ys.add(x)
        }
        for (y in ys) {
            xs.add(y)
        }
    }
}