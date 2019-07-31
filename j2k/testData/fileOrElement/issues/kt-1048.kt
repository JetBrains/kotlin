// ERROR: Not enough information to infer type variable K
// ERROR: Not enough information to infer type variable K
import java.util.HashMap

internal class G<T : String>(t: T)

class Java {
    internal fun test() {
        val m = HashMap()
        m.put(1, 1)
    }

    internal fun test2() {
        val m = HashMap()
        val g = G("")
        val g2 = G("")
    }
}