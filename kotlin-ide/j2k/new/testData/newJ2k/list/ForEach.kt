// ERROR: Unresolved reference: ELinkedList
import java.util.ArrayList

class ForEach {
    fun test() {
        val xs = ArrayList<Any>()
        val ys: MutableList<Any> = ELinkedList<Any>()
        for (x in xs) {
            ys.add(x)
        }
        for (y in ys) {
            xs.add(y)
        }
    }
}