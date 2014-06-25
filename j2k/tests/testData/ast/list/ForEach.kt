import java.util.*

public class ForEach {
    public fun test() {
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