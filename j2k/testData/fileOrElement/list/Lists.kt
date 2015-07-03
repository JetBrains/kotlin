// ERROR: Unresolved reference: LinkedList
// ERROR: None of the following functions can be called with the arguments supplied:  public open fun add(e: kotlin.Any): kotlin.Boolean defined in java.util.ArrayList public open fun add(index: kotlin.Int, element: kotlin.Any): kotlin.Unit defined in java.util.ArrayList
// ERROR: None of the following functions can be called with the arguments supplied:  public open fun add(e: kotlin.Any): kotlin.Boolean defined in java.util.ArrayList public open fun add(index: kotlin.Int, element: kotlin.Any): kotlin.Unit defined in java.util.ArrayList
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